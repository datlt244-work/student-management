package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
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
    private final SemesterRepository semesterRepository;

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
        for (ScheduledClass scheduledClass : openClasses) {
            String classKey = CLASS_KEY_PREFIX + scheduledClass.getClassId();

            // Đếm SV đã đăng ký (snapshot hiện tại từ DB)
            long currentEnrolled = enrollmentRepository
                    .countByScheduledClassClassId(scheduledClass.getClassId());

            // Build hash data
            Map<String, String> classData = new HashMap<>();
            classData.put("classId", String.valueOf(scheduledClass.getClassId()));
            classData.put("courseCode", scheduledClass.getCourse().getCode());
            classData.put("courseName", scheduledClass.getCourse().getName());
            classData.put("credits", String.valueOf(scheduledClass.getCourse().getCredits()));
            classData.put("teacherName", getTeacherName(scheduledClass));
            classData.put("maxSlot", String.valueOf(scheduledClass.getMaxStudents()));
            classData.put("currentSlot", String.valueOf(currentEnrolled));
            classData.put("departmentId", scheduledClass.getCourse().getDepartment() != null
                    ? String.valueOf(scheduledClass.getCourse().getDepartment().getDepartmentId())
                    : "");
            classData.put("status", "OPEN");
            classData.put("sessions", serializeSessions(scheduledClass.getSessions()));

            // 4. Lưu Hash vào Redis
            redisTemplate.opsForHash().putAll(classKey, classData);

            // 5. Thêm classId vào SET của semester
            redisTemplate.opsForSet().add(semesterSetKey, String.valueOf(scheduledClass.getClassId()));

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

    // ===== Phase 3: Atomic slot reservation =====

    /**
     * Kiểm tra class có trong Redis cache không.
     */
    public boolean isClassCached(Integer classId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(CLASS_KEY_PREFIX + classId));
    }

    /**
     * Lấy maxSlot từ Redis cache.
     * 
     * @return maxSlot, hoặc null nếu cache miss
     */
    public Integer getMaxSlot(Integer classId) {
        Object val = redisTemplate.opsForHash().get(CLASS_KEY_PREFIX + classId, "maxSlot");
        if (val == null)
            return null;
        return Integer.parseInt(val.toString());
    }

    /**
     * Atomic slot reservation bằng Redis HINCRBY.
     *
     * <p>
     * Flow:
     * </p>
     * <ol>
     * <li>HINCRBY class:{id} currentSlot 1 → trả về newSlot</li>
     * <li>HGET class:{id} maxSlot</li>
     * <li>Nếu newSlot > maxSlot → HINCRBY -1 (rollback) → return false</li>
     * <li>Nếu newSlot <= maxSlot → return true (đã giữ chỗ thành công)</li>
     * </ol>
     *
     * <p>
     * Tại sao atomic?
     * </p>
     * Redis HINCRBY là single-threaded, O(1), không race condition.
     * Dù 5000 SV gọi đồng thời, mỗi HINCRBY đều sequential →
     * chỉ đúng maxSlot SV được accepted, phần còn lại bị reject ngay lập tức.
     *
     * @param classId ID lớp cần reserve
     * @return true nếu reserve thành công, false nếu hết chỗ
     */
    public boolean reserveSlot(Integer classId) {
        String classKey = CLASS_KEY_PREFIX + classId;

        // 1. Atomic increment
        Long newSlot = redisTemplate.opsForHash().increment(classKey, "currentSlot", 1);

        // 2. Lấy maxSlot
        Object maxSlotObj = redisTemplate.opsForHash().get(classKey, "maxSlot");
        if (maxSlotObj == null) {
            // Cache miss — rollback và return false
            redisTemplate.opsForHash().increment(classKey, "currentSlot", -1);
            return false;
        }
        long maxSlot = Long.parseLong(maxSlotObj.toString());

        // 3. Check vượt quá → rollback
        if (newSlot > maxSlot) {
            redisTemplate.opsForHash().increment(classKey, "currentSlot", -1);
            log.debug("Slot reservation REJECTED for class {}: {}/{}", classId, newSlot, maxSlot);
            return false;
        }

        log.debug("Slot reservation OK for class {}: {}/{}", classId, newSlot, maxSlot);
        return true;
    }

    /**
     * Giải phóng slot khi SV hủy đăng ký hoặc khi validation fail sau reserve.
     * HINCRBY class:{id} currentSlot -1
     */
    public void releaseSlot(Integer classId) {
        String classKey = CLASS_KEY_PREFIX + classId;
        Long newSlot = redisTemplate.opsForHash().increment(classKey, "currentSlot", -1);
        // Đảm bảo không xuống dưới 0
        if (newSlot != null && newSlot < 0) {
            redisTemplate.opsForHash().put(classKey, "currentSlot", "0");
        }
        log.debug("Slot released for class {}, currentSlot: {}", classId, newSlot);
    }

    // ===== Phase 4: Enrollment Stats from Redis =====

    /**
     * Lấy thống kê enrollment real-time từ Redis cho admin dashboard.
     * Toàn bộ dữ liệu đọc từ Redis, KHÔNG query DB.
     *
     * @param semesterId ID semester cần thống kê
     * @return EnrollmentStatsResponse hoặc null nếu cache chưa active
     */
    public com.newwave.student_management.domains.enrollment.dto.response.EnrollmentStatsResponse getEnrollmentStats(
            Integer semesterId) {

        // Check DB first to see if it is published
        Semester semester = semesterRepository.findById(semesterId).orElse(null);
        boolean isPublished = semester != null && semester
                .getEnrollmentStatus() == com.newwave.student_management.domains.profile.entity.EnrollmentStatus.PUBLISHED;

        String semesterSetKey = SEMESTER_KEY_PREFIX + semesterId + ":classes";
        Set<Object> classIds = redisTemplate.opsForSet().members(semesterSetKey);

        if (!isPublished) {
            return null; // Trả về null để Controller biết là Cache Not Active
        }

        if (classIds == null || classIds.isEmpty()) {
            // Đã publish nhưng không có class nào được tạo hoặc có class nhưng chưa open
            return com.newwave.student_management.domains.enrollment.dto.response.EnrollmentStatsResponse.builder()
                    .totalClasses(0)
                    .totalSlots(0)
                    .filledSlots(0)
                    .fillRate("0%")
                    .cacheActive(true) // Set to true since it IS published
                    .classes(java.util.List.of())
                    .build();
        }

        List<com.newwave.student_management.domains.enrollment.dto.response.ClassEnrollmentStatResponse> classStats = new ArrayList<>();
        int totalSlots = 0;
        int filledSlots = 0;

        for (Object classIdObj : classIds) {
            String classKey = CLASS_KEY_PREFIX + classIdObj.toString();
            Map<Object, Object> classData = redisTemplate.opsForHash().entries(classKey);
            if (classData.isEmpty())
                continue;

            int maxSlot = parseIntSafe(classData.get("maxSlot"), 0);
            int currentSlot = parseIntSafe(classData.get("currentSlot"), 0);

            totalSlots += maxSlot;
            filledSlots += currentSlot;

            String fillRate = maxSlot > 0
                    ? String.format("%.1f%%", (currentSlot * 100.0 / maxSlot))
                    : "0%";

            classStats.add(
                    com.newwave.student_management.domains.enrollment.dto.response.ClassEnrollmentStatResponse.builder()
                            .classId(parseIntSafe(classData.get("classId"), 0))
                            .courseCode(getStringSafe(classData.get("courseCode")))
                            .courseName(getStringSafe(classData.get("courseName")))
                            .teacherName(getStringSafe(classData.get("teacherName")))
                            .maxSlot(maxSlot)
                            .currentSlot(currentSlot)
                            .fillRate(fillRate)
                            .status(getStringSafe(classData.get("status")))
                            .build());
        }

        // Sắp xếp theo fill rate giảm dần (lớp đầy nhất ở trên)
        classStats.sort((a, b) -> {
            double rateA = a.getMaxSlot() > 0 ? (double) a.getCurrentSlot() / a.getMaxSlot() : 0;
            double rateB = b.getMaxSlot() > 0 ? (double) b.getCurrentSlot() / b.getMaxSlot() : 0;
            return Double.compare(rateB, rateA);
        });

        String overallFillRate = totalSlots > 0
                ? String.format("%.1f%%", (filledSlots * 100.0 / totalSlots))
                : "0%";

        return com.newwave.student_management.domains.enrollment.dto.response.EnrollmentStatsResponse.builder()
                .totalClasses(classStats.size())
                .totalSlots(totalSlots)
                .filledSlots(filledSlots)
                .fillRate(overallFillRate)
                .cacheActive(true)
                .classes(classStats)
                .build();
    }

    private int parseIntSafe(Object val, int defaultVal) {
        if (val == null)
            return defaultVal;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException ex) {
            return defaultVal;
        }
    }

    private String getStringSafe(Object val) {
        return val != null ? val.toString() : "";
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
                .map(session -> String.format(
                        "{\"dayOfWeek\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\",\"roomName\":\"%s\",\"roomId\":%s}",
                        session.getDayOfWeek(),
                        session.getStartTime(),
                        session.getEndTime(),
                        session.getRoom() != null ? session.getRoom().getName() : "N/A",
                        session.getRoom() != null ? session.getRoom().getRoomId() : "null"))
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
