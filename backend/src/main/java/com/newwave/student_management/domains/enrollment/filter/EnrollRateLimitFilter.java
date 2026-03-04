package com.newwave.student_management.domains.enrollment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * EnrollRateLimitFilter — Rate limiting cho endpoint đăng ký / hủy đăng ký tín
 * chỉ.
 *
 * Cơ chế: INCR + TTL trên Redis.
 * key = rate:enroll:{userId} TTL = WINDOW_SECONDS.
 * Giới hạn MAX_REQUESTS request trong WINDOW_SECONDS giây mỗi sinh viên.
 */
// KHÔNG dùng @Component — filter được đăng ký thủ công trong SecurityConfig
// (addFilterAfter BearerTokenAuthenticationFilter) để chạy SAU khi JWT đã được
// giải mã.
// Nếu đánh @Component, Spring Boot sẽ tự đăng ký thêm 1 lần nữa khiến filter
// chạy 2 lần.
@RequiredArgsConstructor
@Slf4j
public class EnrollRateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "rate:enroll:";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean isEnrollEndpoint = path.contains("/student/classes/")
                && path.endsWith("/enroll")
                && (method.equals("POST") || method.equals("DELETE"));
        return !isEnrollEndpoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String limitKey = resolveUserId(request);
        String redisKey = KEY_PREFIX + limitKey;

        Long count = stringRedisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1L) {
            // Lần đầu tiên trong window: set TTL
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS));
        } else if (count != null) {
            // Safety net: nếu TTL bị mất (server crash giữa INCR và EXPIRE lần trước)
            // getExpire trả về -1 khi key tồn tại nhưng KHÔNG có TTL
            // → re-set TTL để tránh user bị block vĩnh viễn
            Long ttl = stringRedisTemplate.getExpire(redisKey);
            if (ttl != null && ttl == -1L) {
                log.warn("Rate limit key {} has no TTL (possible crash recovery), resetting TTL", redisKey);
                stringRedisTemplate.expire(redisKey, Duration.ofSeconds(WINDOW_SECONDS));
            }
        }

        if (count != null && count > MAX_REQUESTS) {
            log.warn("Rate limit exceeded for key={}, count={}/{} per {}s",
                    limitKey, count, MAX_REQUESTS, WINDOW_SECONDS);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"code\":4029,\"message\":\"Qua nhieu yeu cau dang ky. Vui long cho "
                            + WINDOW_SECONDS + " giay truoc khi thu lai.\",\"result\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Lấy userId từ JWT claim (đã xác thực bởi Spring Security).
     * Fallback về IP nếu chưa xác thực.
     */
    private String resolveUserId(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String userId = jwt.getClaimAsString("userId");
                if (userId != null && !userId.isBlank()) {
                    return userId;
                }
            }
        } catch (Exception ex) {
            log.debug("Could not resolve userId from JWT: {}", ex.getMessage());
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
