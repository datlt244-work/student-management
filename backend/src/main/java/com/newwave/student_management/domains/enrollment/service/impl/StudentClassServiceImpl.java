package com.newwave.student_management.domains.enrollment.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.enrollment.dto.response.ClassSessionResponse;
import com.newwave.student_management.domains.enrollment.dto.response.StudentAvailableClassResponse;
import com.newwave.student_management.domains.enrollment.dto.response.StudentEnrolledClassResponse;
import com.newwave.student_management.domains.enrollment.entity.ClassSession;
import com.newwave.student_management.domains.enrollment.entity.Enrollment;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.repository.EnrollmentRepository;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.enrollment.service.IStudentClassService;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentClassServiceImpl implements IStudentClassService {

        private final ScheduledClassRepository scheduledClassRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final StudentRepository studentRepository;
        private final SemesterRepository semesterRepository;
        private final ClassCacheService classCacheService;

        @Override
        @Transactional(readOnly = true)
        public List<StudentAvailableClassResponse> getAvailableClasses(UUID userId) {
                Student student = studentRepository.findByUserIdWithDepartment(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

                Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

                Integer departmentId = student.getDepartment() != null ? student.getDepartment().getDepartmentId()
                                : null;
                if (departmentId == null) {
                        return List.of();
                }

                if (currentSemester
                                .getEnrollmentStatus() != com.newwave.student_management.domains.profile.entity.EnrollmentStatus.PUBLISHED) {
                        return List.of();
                }

                // Phase 2: Redis-first → fallback DB
                List<StudentAvailableClassResponse> cachedResult = null;
                try {
                        cachedResult = classCacheService.getAvailableClassesFromCache(
                                        currentSemester.getSemesterId(), departmentId, student.getStudentId());
                } catch (Exception ex) {
                        log.warn("Redis cache read failed, falling back to DB: {}", ex.getMessage());
                }

                if (cachedResult != null) {
                        log.debug("Cache HIT: returned {} classes from Redis", cachedResult.size());
                        return cachedResult;
                }

                // Cache MISS → fallback to PostgreSQL
                log.debug("Cache MISS: querying PostgreSQL for available classes");
                List<ScheduledClass> availableClasses = scheduledClassRepository.findAvailableClassesForStudent(
                                departmentId,
                                currentSemester.getSemesterId(),
                                student.getStudentId());

                if (availableClasses.isEmpty()) {
                        return List.of();
                }

                // Batch count — 1 query duy nhất thay vì N query
                List<Integer> classIds = availableClasses.stream()
                                .map(ScheduledClass::getClassId)
                                .collect(Collectors.toList());
                List<Object[]> countRows = scheduledClassRepository.countEnrollmentsByClassIds(classIds);
                java.util.Map<Integer, Long> enrollCountMap = new java.util.HashMap<>();
                for (Object[] row : countRows) {
                        enrollCountMap.put((Integer) row[0], (Long) row[1]);
                }

                return availableClasses.stream()
                                .map(sc -> mapToAvailableClassResponse(sc,
                                                enrollCountMap.getOrDefault(sc.getClassId(), 0L)))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<StudentEnrolledClassResponse> getEnrolledClasses(UUID userId) {
                Student student = studentRepository.findByUserIdWithDepartment(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

                Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

                List<Enrollment> enrollments = enrollmentRepository
                                .findByStudentStudentIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                                                student.getStudentId(),
                                                currentSemester.getSemesterId());

                return enrollments.stream()
                                .map(this::mapToEnrolledClassResponse)
                                .collect(Collectors.toList());
        }

        private StudentEnrolledClassResponse mapToEnrolledClassResponse(Enrollment enrollment) {
                ScheduledClass scheduledClass = enrollment.getScheduledClass();
                String teacherName = "N/A";
                if (scheduledClass.getTeacher() != null) {
                        teacherName = (scheduledClass.getTeacher().getFirstName() + " "
                                        + scheduledClass.getTeacher().getLastName())
                                        .trim();
                }

                return StudentEnrolledClassResponse.builder()
                                .enrollmentId(enrollment.getEnrollmentId())
                                .classId(scheduledClass.getClassId())
                                .courseCode(scheduledClass.getCourse().getCode())
                                .courseName(scheduledClass.getCourse().getName())
                                .credits(scheduledClass.getCourse().getCredits())
                                .teacherName(teacherName)
                                .sessions(scheduledClass.getSessions() != null
                                                ? scheduledClass.getSessions().stream()
                                                                .map(session -> ClassSessionResponse
                                                                                .builder()
                                                                                .sessionId(session.getSessionId())
                                                                                .roomId(session.getRoom() != null
                                                                                                ? session.getRoom()
                                                                                                                .getRoomId()
                                                                                                : null)
                                                                                .roomName(session.getRoom() != null
                                                                                                ? session.getRoom()
                                                                                                                .getName()
                                                                                                : null)
                                                                                .dayOfWeek(session.getDayOfWeek())
                                                                                .startTime(session.getStartTime())
                                                                                .endTime(session.getEndTime())
                                                                                .build())
                                                                .collect(Collectors.toList())
                                                : new java.util.ArrayList<>())
                                .enrollmentDate(enrollment.getEnrollmentDate())
                                .status(enrollment.getStatus())
                                .build();
        }

        private StudentAvailableClassResponse mapToAvailableClassResponse(ScheduledClass scheduledClass,
                        long currentStudents) {
                String teacherName = "N/A";
                if (scheduledClass.getTeacher() != null) {
                        teacherName = (scheduledClass.getTeacher().getFirstName() + " "
                                        + scheduledClass.getTeacher().getLastName())
                                        .trim();
                }

                return StudentAvailableClassResponse.builder()
                                .classId(scheduledClass.getClassId())
                                .courseCode(scheduledClass.getCourse().getCode())
                                .courseName(scheduledClass.getCourse().getName())
                                .credits(scheduledClass.getCourse().getCredits())
                                .teacherName(teacherName)
                                .sessions(scheduledClass.getSessions() != null
                                                ? scheduledClass.getSessions().stream()
                                                                .map(session -> ClassSessionResponse
                                                                                .builder()
                                                                                .sessionId(session.getSessionId())
                                                                                .roomId(session.getRoom() != null
                                                                                                ? session.getRoom()
                                                                                                                .getRoomId()
                                                                                                : null)
                                                                                .roomName(session.getRoom() != null
                                                                                                ? session.getRoom()
                                                                                                                .getName()
                                                                                                : null)
                                                                                .dayOfWeek(session.getDayOfWeek())
                                                                                .startTime(session.getStartTime())
                                                                                .endTime(session.getEndTime())
                                                                                .build())
                                                                .collect(Collectors.toList())
                                                : new java.util.ArrayList<>())
                                .maxStudents(scheduledClass.getMaxStudents())
                                .currentStudents((int) currentStudents)
                                .status(scheduledClass.getStatus())
                                .build();
        }

        @Override
        @Transactional
        public void enroll(UUID userId, Integer classId) {
                Student student = studentRepository.findByUserIdWithDepartment(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

                ScheduledClass scheduledClass = scheduledClassRepository.findByClassIdAndDeletedAtIsNull(classId)
                                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

                // 1. Check if class is OPEN and Semester is PUBLISHED
                if (scheduledClass
                                .getStatus() != com.newwave.student_management.domains.enrollment.entity.ScheduledClassStatus.OPEN) {
                        throw new AppException(ErrorCode.CLASS_NOT_OPEN);
                }

                if (scheduledClass.getSemester()
                                .getEnrollmentStatus() != com.newwave.student_management.domains.profile.entity.EnrollmentStatus.PUBLISHED) {
                        throw new AppException(ErrorCode.CLASS_NOT_OPEN); // Reusing CLASS_NOT_OPEN or create a new one,
                                                                          // but for now this works to prevent
                                                                          // registration
                }

                // 2. Check if already enrolled in THIS class
                if (enrollmentRepository.existsByScheduledClassClassIdAndStudentStudentId(classId,
                                student.getStudentId())) {
                        throw new AppException(ErrorCode.STUDENT_ALREADY_ENROLLED);
                }

                // Check Waitlist conflict
                if (enrollmentRepository.findByScheduledClassClassIdAndStudentStudentIdWaitlisted(classId,
                                student.getStudentId()).isPresent()) {
                        throw new AppException(ErrorCode.STUDENT_ALREADY_WAITLISTED);
                }

                // 3. Check if already enrolled in another class of the SAME COURSE in the SAME
                // SEMESTER
                if (enrollmentRepository
                                .existsByStudentStudentIdAndScheduledClassCourseCourseIdAndScheduledClassSemesterSemesterIdAndScheduledClassDeletedAtIsNull(
                                                student.getStudentId(), scheduledClass.getCourse().getCourseId(),
                                                scheduledClass.getSemester().getSemesterId())) {
                        throw new AppException(ErrorCode.COURSE_ALREADY_ENROLLED);
                }

                // 4. Check capacity — Redis-first (atomic), fallback DB with Pessimistic Lock
                boolean usedRedisReservation = false;
                try {
                        if (classCacheService.isClassCached(classId)) {
                                // Redis path: HINCRBY atomic — race-condition-free
                                boolean reserved = classCacheService.reserveSlot(classId);
                                if (!reserved) {
                                        boolean joinedWaitlist = classCacheService.joinWaitlist(classId,
                                                        student.getStudentId());
                                        if (!joinedWaitlist) {
                                                throw new AppException(ErrorCode.CLASS_FULL);
                                        }
                                        log.info("Student {} joined waitlist for class {}", student.getStudentId(),
                                                        classId);
                                        Enrollment waitlistEnrollment = new Enrollment();
                                        waitlistEnrollment.setStudent(student);
                                        waitlistEnrollment.setScheduledClass(scheduledClass);
                                        waitlistEnrollment.setEnrollmentDate(java.time.LocalDate.now());
                                        waitlistEnrollment.setStatus(
                                                        com.newwave.student_management.domains.enrollment.entity.EnrollmentRecordStatus.WAITLISTED);
                                        enrollmentRepository.save(waitlistEnrollment);
                                        return;
                                }
                                usedRedisReservation = true;
                                log.debug("Slot reserved via Redis for class {}", classId);
                        } else {
                                // DB fallback: SELECT ... FOR UPDATE (Pessimistic Lock)
                                ScheduledClass lockedClass = scheduledClassRepository
                                                .findByClassIdWithLock(classId)
                                                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
                                long currentEnrolled = enrollmentRepository.countByScheduledClassClassId(classId);
                                if (currentEnrolled >= lockedClass.getMaxStudents()) {
                                        throw new AppException(ErrorCode.CLASS_FULL);
                                }
                        }
                } catch (AppException ex) {
                        throw ex; // Re-throw business exceptions
                } catch (Exception ex) {
                        // Redis error → fallback DB with lock
                        log.warn("Redis slot check failed, falling back to DB with lock: {}", ex.getMessage());
                        ScheduledClass lockedClass = scheduledClassRepository
                                        .findByClassIdWithLock(classId)
                                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
                        long currentEnrolled = enrollmentRepository.countByScheduledClassClassId(classId);
                        if (currentEnrolled >= lockedClass.getMaxStudents()) {
                                throw new AppException(ErrorCode.CLASS_FULL);
                        }
                }

                // 5. Check schedule conflicts (always DB — nhẹ, chỉ chạy khi slot OK)
                try {
                        if (scheduledClass.getSessions() != null) {
                                for (ClassSession session : scheduledClass.getSessions()) {
                                        long conflicts = enrollmentRepository.countStudentConflicts(
                                                        student.getStudentId(),
                                                        scheduledClass.getSemester().getSemesterId(),
                                                        session.getDayOfWeek(),
                                                        session.getStartTime(),
                                                        session.getEndTime());
                                        if (conflicts > 0) {
                                                throw new AppException(ErrorCode.STUDENT_SCHEDULE_CONFLICT);
                                        }
                                }
                        }
                } catch (AppException ex) {
                        // Validation failed AFTER Redis reservation → release slot
                        if (usedRedisReservation) {
                                try {
                                        classCacheService.releaseSlot(classId);
                                        log.debug("Released Redis slot for class {} due to schedule conflict", classId);
                                } catch (Exception innerEx) {
                                        log.error("Failed to release Redis slot for class {}: {}", classId,
                                                        innerEx.getMessage());
                                }
                        }
                        throw ex;
                }

                // 6. Create enrollment (sync → DB)
                com.newwave.student_management.domains.enrollment.entity.Enrollment enrollment = new com.newwave.student_management.domains.enrollment.entity.Enrollment();
                enrollment.setStudent(student);
                enrollment.setScheduledClass(scheduledClass);
                enrollment.setEnrollmentDate(java.time.LocalDate.now());

                try {
                        enrollmentRepository.save(enrollment);
                        log.info("Student {} enrolled in class {} (Redis reservation: {})",
                                        student.getStudentId(), classId, usedRedisReservation);
                } catch (RuntimeException ex) {
                        if (usedRedisReservation) {
                                try {
                                        classCacheService.releaseSlot(classId);
                                        log.debug("Released Redis slot for class {} due to DB save failure", classId);
                                } catch (Exception innerEx) {
                                        log.error("Failed to release Redis slot for class {}: {}", classId,
                                                        innerEx.getMessage());
                                }
                        }
                        throw ex;
                }
        }

        @Override
        @Transactional
        public void unenroll(UUID userId, Integer classId) {
                Student student = studentRepository.findByUserIdWithDepartment(userId)
                                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

                // Find the enrollment record
                Enrollment enrollment = enrollmentRepository
                                .findByScheduledClassClassIdAndStudentStudentId(classId, student.getStudentId())
                                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

                // Verify the class belongs to the current semester
                Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                                .orElseThrow(() -> new AppException(ErrorCode.NO_CURRENT_SEMESTER));

                if (!enrollment.getScheduledClass().getSemester().getSemesterId()
                                .equals(currentSemester.getSemesterId())) {
                        throw new AppException(ErrorCode.SEMESTER_NOT_CURRENT);
                }

                // Guard: Chỉ cho phép hủy khi cổng đăng ký đang mở (PUBLISHED)
                if (currentSemester
                                .getEnrollmentStatus() != com.newwave.student_management.domains.profile.entity.EnrollmentStatus.PUBLISHED) {
                        throw new AppException(ErrorCode.ENROLLMENT_CLOSED);
                }

                enrollment.setStatus(
                                com.newwave.student_management.domains.enrollment.entity.EnrollmentRecordStatus.DROPPED);
                enrollmentRepository.save(enrollment);

                // Release Redis slot or autofill (nếu cache đang active)
                try {
                        if (classCacheService.isClassCached(classId)) {
                                autoFillFromWaitlist(classId, enrollment.getScheduledClass());
                        }
                } catch (Exception ex) {
                        log.warn("Failed to auto-fill or release Redis slot for class {} after unenroll: {}", classId,
                                        ex.getMessage());
                }
        }

        private void autoFillFromWaitlist(Integer classId, ScheduledClass scheduledClass) {
                while (true) {
                        UUID waitlistedStudentId = classCacheService.popWaitlist(classId);
                        if (waitlistedStudentId == null) {
                                // No one in waitlist, release slot
                                classCacheService.releaseSlot(classId);
                                log.debug("Released Redis slot for class {} after unenroll", classId);
                                break;
                        }

                        // Try to enroll this waitlisted student
                        Student waitlistedStudent = studentRepository.findById(waitlistedStudentId).orElse(null);
                        if (waitlistedStudent == null)
                                continue;

                        boolean hasConflict = false;
                        if (scheduledClass.getSessions() != null) {
                                for (ClassSession session : scheduledClass.getSessions()) {
                                        long conflicts = enrollmentRepository.countStudentConflicts(
                                                        waitlistedStudentId,
                                                        scheduledClass.getSemester().getSemesterId(),
                                                        session.getDayOfWeek(),
                                                        session.getStartTime(),
                                                        session.getEndTime());
                                        if (conflicts > 0) {
                                                hasConflict = true;
                                                break;
                                        }
                                }
                        }

                        if (hasConflict) {
                                enrollmentRepository.findByScheduledClassClassIdAndStudentStudentIdWaitlisted(classId,
                                                waitlistedStudentId).ifPresent(e -> {
                                                        e.setStatus(com.newwave.student_management.domains.enrollment.entity.EnrollmentRecordStatus.DROPPED);
                                                        enrollmentRepository.save(e);
                                                });
                                continue;
                        }

                        // Success! Update status to ENROLLED
                        enrollmentRepository.findByScheduledClassClassIdAndStudentStudentIdWaitlisted(classId,
                                        waitlistedStudentId).ifPresentOrElse(e -> {
                                                e.setStatus(com.newwave.student_management.domains.enrollment.entity.EnrollmentRecordStatus.ENROLLED);
                                                enrollmentRepository.save(e);
                                        }, () -> {
                                                Enrollment newEnrollment = new Enrollment();
                                                newEnrollment.setStudent(waitlistedStudent);
                                                newEnrollment.setScheduledClass(scheduledClass);
                                                newEnrollment.setEnrollmentDate(java.time.LocalDate.now());
                                                enrollmentRepository.save(newEnrollment);
                                        });

                        log.info("Auto-filled slot for class {} with waitlisted student {}", classId,
                                        waitlistedStudentId);
                        break;
                }
        }

        @Override
        @Transactional(readOnly = true)
        public List<com.newwave.student_management.domains.enrollment.dto.response.StudentClassMemberResponse> getClassMembers(
                        Integer classId) {
                // Find all enrollments for this class ID that are active
                List<Enrollment> enrollments = enrollmentRepository.findByScheduledClassClassId(classId);

                return enrollments.stream()
                                .map(enrollment -> com.newwave.student_management.domains.enrollment.dto.response.StudentClassMemberResponse
                                                .builder()
                                                .studentCode(enrollment.getStudent().getStudentCode())
                                                .firstName(enrollment.getStudent().getFirstName())
                                                .lastName(enrollment.getStudent().getLastName())
                                                .email(enrollment.getStudent().getEmail())
                                                .build())
                                .collect(Collectors.toList());
        }
}
