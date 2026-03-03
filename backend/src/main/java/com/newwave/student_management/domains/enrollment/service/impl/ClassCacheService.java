package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ClassCacheService — Service đồng bộ dữ liệu lớp học lên Redis.
 *
 * <h3>Tại sao cần service này?</h3>
 * Khi hàng ngàn sinh viên đăng ký tín chỉ đồng thời, việc query trực tiếp
 * PostgreSQL (JOIN 4 bảng, COUNT enrollment) sẽ gây quá tải.
 * Service này đẩy trước (pre-warm) dữ liệu lớp học lên Redis dưới dạng Hash,
 * giúp:
 * <ul>
 * <li>Đọc danh sách lớp: O(1) từ Redis thay vì JOIN PostgreSQL</li>
 * <li>Check slot: HGET atomic thay vì COUNT query</li>
 * <li>Reserve slot: HINCRBY atomic, không race condition</li>
 * </ul>
 *
 * <h3>Redis Key Structure</h3>
 * 
 * <pre>
 * semester:{semesterId}:classes → SET [classId1, classId2, ...]
 * class:{classId}               → HASH {classId, courseCode, courseName, ...}
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ScheduledClassRepository scheduledClassRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * Prefix cho Redis keys.
     * - SEMESTER_KEY_PREFIX + semesterId + ":classes" → SET chứa tất cả classId
     * - CLASS_KEY_PREFIX + classId → HASH chứa chi tiết lớp
     */
    private static final String SEMESTER_KEY_PREFIX = "semester:";
    private static final String CLASS_KEY_PREFIX = "class:";

    /**
     * Đồng bộ tất cả lớp OPEN trong 1 semester lên Redis.
     *
     * <p>
     * Flow:
     * </p>
     * <ol>
     * <li>Query PostgreSQL: lấy tất cả lớp OPEN chưa bị xóa</li>
     * <li>Với mỗi lớp: tạo Redis Hash chứa thông tin (courseCode, maxSlot,
     * currentSlot, sessions...)</li>
     * <li>Thêm classId vào SET semester:{id}:classes</li>
     * </ol>
     *
     * @param semesterId ID của semester cần publish
     * @return số lượng lớp đã sync lên Redis
     */
    @Transactional(readOnly = true)
    public int warmCache(Integer semesterId) {
        // 1. Lấy tất cả lớp OPEN trong semester
        List<ScheduledClass> openClasses = scheduledClassRepository
                .findBySemesterSemesterIdAndStatusAndDeletedAtIsNull(
                        semesterId, ScheduledClassStatus.OPEN);

        String semesterSetKey = SEMESTER_KEY_PREFIX + semesterId + ":classes";

        // 2. Xóa cache cũ (nếu re-publish)
        clearSemesterCache(semesterId, semesterSetKey);

        // 3. Sync từng lớp lên Redis
        int count = 0;
        for (ScheduledClass sc : openClasses) {
            String classKey = CLASS_KEY_PREFIX + sc.getClassId();

            // Đếm SV đã đăng ký (snapshot hiện tại từ DB)
            long currentEnrolled = enrollmentRepository
                    .countByScheduledClassClassId(sc.getClassId());

            // Build hash data
            Map<String, String> classData = new HashMap<>();
            classData.put("classId", String.valueOf(sc.getClassId()));
            classData.put("courseCode", sc.getCourse().getCode());
            classData.put("courseName", sc.getCourse().getName());
            classData.put("credits", String.valueOf(sc.getCourse().getCredits()));
            classData.put("teacherName", getTeacherName(sc));
            classData.put("maxSlot", String.valueOf(sc.getMaxStudents()));
            classData.put("currentSlot", String.valueOf(currentEnrolled));
            classData.put("departmentId", sc.getCourse().getDepartment() != null
                    ? String.valueOf(sc.getCourse().getDepartment().getDepartmentId())
                    : "");
            classData.put("status", "OPEN");
            classData.put("sessions", serializeSessions(sc.getSessions()));

            // 4. Lưu Hash vào Redis
            redisTemplate.opsForHash().putAll(classKey, classData);

            // 5. Thêm classId vào SET của semester
            redisTemplate.opsForSet().add(semesterSetKey, String.valueOf(sc.getClassId()));

            count++;
        }

        log.info("Cache warmed for semester {}: {} classes synced to Redis", semesterId, count);
        return count;
    }

    /**
     * Xóa toàn bộ cache của 1 semester.
     * Gọi trước khi re-publish hoặc khi đóng đăng ký (CLOSED).
     */
    public void clearSemesterCache(Integer semesterId) {
        String semesterSetKey = SEMESTER_KEY_PREFIX + semesterId + ":classes";
        clearSemesterCache(semesterId, semesterSetKey);
    }

    private void clearSemesterCache(Integer semesterId, String semesterSetKey) {
        Set<Object> existingClassIds = redisTemplate.opsForSet().members(semesterSetKey);
        if (existingClassIds != null && !existingClassIds.isEmpty()) {
            for (Object classId : existingClassIds) {
                redisTemplate.delete(CLASS_KEY_PREFIX + classId);
            }
            redisTemplate.delete(semesterSetKey);
            log.info("Cleared {} cached classes for semester {}", existingClassIds.size(), semesterId);
        }
    }

    /**
     * Kiểm tra semester đã được cache trên Redis chưa.
     */
    public boolean isCached(Integer semesterId) {
        String semesterSetKey = SEMESTER_KEY_PREFIX + semesterId + ":classes";
        Long size = redisTemplate.opsForSet().size(semesterSetKey);
        return size != null && size > 0;
    }

    /**
     * Lấy số lượng lớp đang cache cho 1 semester.
     */
    public long getCachedClassCount(Integer semesterId) {
        String semesterSetKey = SEMESTER_KEY_PREFIX + semesterId + ":classes";
        Long size = redisTemplate.opsForSet().size(semesterSetKey);
        return size != null ? size : 0;
    }

    // ===== Helper methods =====

    private String getTeacherName(ScheduledClass sc) {
        if (sc.getTeacher() == null)
            return "N/A";
        return (sc.getTeacher().getFirstName() + " " + sc.getTeacher().getLastName()).trim();
    }

    /**
     * Serialize sessions thành JSON string đơn giản để lưu vào Redis Hash.
     * Format: "[{day:2,start:'08:00',end:'10:00',room:'A-101'}, ...]"
     */
    private String serializeSessions(List<ClassSession> sessions) {
        if (sessions == null || sessions.isEmpty())
            return "[]";

        return "[" + sessions.stream()
                .map(s -> String.format(
                        "{\"dayOfWeek\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomName\":\"%s\",\"roomId\":%s}",
                        s.getDayOfWeek(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getRoom() != null ? s.getRoom().getName() : "N/A",
                        s.getRoom() != null ? s.getRoom().getRoomId() : "null"))
                .collect(Collectors.joining(",")) + "]";
    }

    // ===== Phase 2: Read from cache =====

    /**
     * Đọc danh sách lớp available từ Redis cache.
     *
     * <p>
     * Flow:
     * </p>
     * <ol>
     * <li>Lấy SET semester:{id}:classes → danh sách classId</li>
     * <li>Với mỗi classId: HGETALL class:{id} → Hash data</li>
     * <li>Filter theo departmentId của SV</li>
     * <li>Loại bỏ lớp SV đã đăng ký (check enrollment DB — nhẹ vì chỉ 1 query
     * nhỏ)</li>
     * <li>Map thành StudentAvailableClassResponse</li>
     * </ol>
     *
     * @param semesterId   ID semester hiện tại
     * @param departmentId ID khoa của SV
     * @param studentId    ID SV (để exclude lớp đã đăng ký)
     * @return danh sách lớp available, null nếu cache miss
     */
    public List<com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse> getAvailableClassesFromCache(
            Integer semesterId, Integer departmentId, UUID studentId) {

        String semesterSetKey = SEMESTER_KEY_PREFIX + semesterId + ":classes";
        Set<Object> classIds = redisTemplate.opsForSet().members(semesterSetKey);

        // Cache miss → return null để caller fallback về DB
        if (classIds == null || classIds.isEmpty()) {
            return null;
        }

        // Lấy danh sách classId SV đã đăng ký (1 query nhỏ, rất nhanh)
        Set<Integer> enrolledClassIds = enrollmentRepository
                .findClassIdsByStudentIdAndSemesterId(studentId, semesterId);

        List<com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse> result = new ArrayList<>();

        for (Object classIdObj : classIds) {
            String classKey = CLASS_KEY_PREFIX + classIdObj;
            Map<Object, Object> data = redisTemplate.opsForHash().entries(classKey);

            if (data.isEmpty())
                continue;

            // Filter theo department
            String classDeptId = (String) data.get("departmentId");
            if (classDeptId == null || classDeptId.isEmpty())
                continue;
            if (!classDeptId.equals(String.valueOf(departmentId)))
                continue;

            // Skip lớp SV đã enrolled
            int classId = Integer.parseInt((String) data.get("classId"));
            if (enrolledClassIds.contains(classId))
                continue;

            // Map Redis Hash → Response DTO
            result.add(mapCacheToResponse(data));
        }

        log.debug("Cache hit for semester {}: returned {} available classes for student {}",
                semesterId, result.size(), studentId);
        return result;
    }

    /**
     * Map Redis Hash data → StudentAvailableClassResponse DTO.
     */
    private com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse mapCacheToResponse(
            Map<Object, Object> data) {

        return com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse.builder()
                .classId(Integer.parseInt((String) data.get("classId")))
                .courseCode((String) data.get("courseCode"))
                .courseName((String) data.get("courseName"))
                .credits(Integer.parseInt((String) data.getOrDefault("credits", "0")))
                .teacherName((String) data.get("teacherName"))
                .maxStudents(Integer.parseInt((String) data.getOrDefault("maxSlot", "0")))
                .currentStudents(Integer.parseInt((String) data.getOrDefault("currentSlot", "0")))
                .status(com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus.OPEN)
                .sessions(deserializeSessions((String) data.get("sessions")))
                .build();
    }

    /**
     * Deserialize sessions JSON string từ Redis → List<ClassSessionResponse>.
     * Input format:
     * [{"dayOfWeek":2,"startTime":"08:00","endTime":"10:00","roomName":"A-101","roomId":1},
     * ...]
     */
    private List<com.newwave.student_management.domains.enrollment.dto.response.ClassSessionResponse> deserializeSessions(
            String sessionsJson) {

        if (sessionsJson == null || sessionsJson.equals("[]")) {
            return new ArrayList<>();
        }

        List<com.newwave.student_management.domains.enrollment.dto.response.ClassSessionResponse> result = new ArrayList<>();

        // Simple JSON parsing (tránh phụ thuộc Jackson cho Redis value)
        // Format: [{"dayOfWeek":2,"startTime":"08:00",...}, {...}]
        String content = sessionsJson.substring(1, sessionsJson.length() - 1); // bỏ []
        if (content.isEmpty())
            return result;

        // Split by "},{" pattern
        String[] items = content.split("\\},\\{");
        for (String item : items) {
            String clean = item.replace("{", "").replace("}", "");

            Integer dayOfWeek = null;
            String startTime = null;
            String endTime = null;
            String roomName = null;
            Integer roomId = null;

            for (String pair : clean.split(",")) {
                String[] kv = pair.split(":", 2);
                if (kv.length != 2)
                    continue;
                String key = kv[0].replace("\"", "").trim();
                String val = kv[1].replace("\"", "").trim();

                switch (key) {
                    case "dayOfWeek":
                        dayOfWeek = Integer.parseInt(val);
                        break;
                    case "startTime":
                        startTime = val;
                        break;
                    case "endTime":
                        endTime = val;
                        break;
                    case "roomName":
                        roomName = val;
                        break;
                    case "roomId":
                        if (!"null".equals(val))
                            roomId = Integer.parseInt(val);
                        break;
                }
            }

            result.add(com.newwave.student_management.domains.enrollment.dto.response.ClassSessionResponse.builder()
                    .dayOfWeek(dayOfWeek)
                    .startTime(startTime != null ? java.time.LocalTime.parse(startTime) : null)
                    .endTime(endTime != null ? java.time.LocalTime.parse(endTime) : null)
                    .roomName(roomName)
                    .roomId(roomId)
                    .build());
        }
        return result;
    }
}
