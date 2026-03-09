package com.newwave.student_management.domains.attendance.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.attendance.dto.request.AttendanceAppealRequest;
import com.newwave.student_management.domains.attendance.dto.response.AttendanceAppealResponse;
import com.newwave.student_management.domains.attendance.dto.response.EvidenceUploadResponse;
import com.newwave.student_management.domains.attendance.entity.AttendanceAppeal;
import com.newwave.student_management.domains.attendance.repository.AttendanceAppealRepository;
import com.newwave.student_management.domains.attendance.service.IAttendanceAppealService;
import com.newwave.student_management.domains.enrollment.entity.ScheduledClass;
import com.newwave.student_management.domains.enrollment.repository.ScheduledClassRepository;
import com.newwave.student_management.domains.notification.service.NotificationInternalService;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.entity.Teacher;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.infrastructure.mail.IMailService;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceAppealServiceImpl implements IAttendanceAppealService {

    private final AttendanceAppealRepository appealRepository;
    private final StudentRepository studentRepository;
    private final ScheduledClassRepository classRepository;
    private final IMailService mailService;
    private final NotificationInternalService notificationService;
    private final IStorageService storageService;

    @Override
    @Transactional
    public AttendanceAppealResponse submitAppeal(UUID userId, AttendanceAppealRequest request) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        ScheduledClass scheduledClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        // Check if already appealed
        appealRepository.findByStudent_StudentIdAndScheduledClass_ClassIdAndAttendanceDate(
                student.getStudentId(), request.getClassId(), request.getAttendanceDate())
                .ifPresent(a -> {
                    throw new RuntimeException("ALREADY_APPEALED");
                });

        AttendanceAppeal appeal = AttendanceAppeal.builder()
                .student(student)
                .scheduledClass(scheduledClass)
                .attendanceDate(request.getAttendanceDate())
                .reason(request.getReason())
                .evidenceUrl(request.getEvidenceUrl())
                .status(AttendanceAppeal.AppealStatus.PENDING)
                .build();

        appeal = appealRepository.save(appeal);

        // Send Email and Notification to Teacher
        Teacher teacher = scheduledClass.getTeacher();
        if (teacher != null && teacher.getUser() != null) {
            sendNotifications(student, teacher, scheduledClass, appeal);
        }

        return mapToResponse(appeal);
    }

    @Override
    @Transactional
    public EvidenceUploadResponse uploadEvidence(UUID userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("FILE_EMPTY");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String objectName = "appeals/" + userId + "/" + UUID.randomUUID() + extension;

            String publicUrl = storageService.uploadFile(
                    objectName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType());

            return new EvidenceUploadResponse(publicUrl);
        } catch (Exception e) {
            log.error("Failed to upload evidence: {}", e.getMessage());
            throw new RuntimeException("UPLOAD_FAILED");
        }
    }

    private void sendNotifications(Student student, Teacher teacher, ScheduledClass scheduledClass,
            AttendanceAppeal appeal) {
        String studentName = student.getFirstName() + " " + student.getLastName();
        String courseName = scheduledClass.getCourse().getName();
        String dateStr = appeal.getAttendanceDate().toString();

        // 1. Send System Notification
        String title = "New Attendance Appeal Request";
        String body = String.format(
                "Student %s has submitted an appeal for class %s on date %s.\nReason: %s",
                studentName, courseName, dateStr, appeal.getReason());
        String actionUrl = appeal.getEvidenceUrl();

        try {
            notificationService.sendToUser(teacher.getUser(), title, body, actionUrl);
        } catch (Exception e) {
            log.error("Failed to send notification to teacher: {}", e.getMessage());
        }

        // 2. Send Email
        String emailSubject = "[Attendance Appeal] - " + courseName + " - " + studentName;
        String emailBody = String.format(
                "Dear %s,\n\n" +
                        "You have a new attendance appeal request from student %s (Code: %s).\n\n" +
                        "Course: %s\n" +
                        "Date: %s\n" +
                        "Reason: %s\n" +
                        "Evidence Image: %s\n\n" +
                        "Please log in to the system to review this request.\n\n" +
                        "Best regards,\nStudent Management System",
                teacher.getFirstName(), studentName, student.getStudentCode(),
                courseName, dateStr, appeal.getReason(), appeal.getEvidenceUrl());

        try {
            mailService.sendMail(teacher.getUser().getEmail(), emailSubject, emailBody, false);
        } catch (Exception e) {
            log.error("Failed to send email to teacher: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceAppealResponse> getStudentAppeals(UUID userId) {
        Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        return appealRepository.findAll().stream()
                .filter(a -> a.getStudent().getStudentId().equals(student.getStudentId()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AttendanceAppealResponse mapToResponse(AttendanceAppeal appeal) {
        return AttendanceAppealResponse.builder()
                .appealId(appeal.getAppealId())
                .classId(appeal.getScheduledClass().getClassId())
                .courseName(appeal.getScheduledClass().getCourse().getName())
                .attendanceDate(appeal.getAttendanceDate())
                .reason(appeal.getReason())
                .evidenceUrl(appeal.getEvidenceUrl())
                .status(appeal.getStatus())
                .teacherResponse(appeal.getTeacherResponse())
                .createdAt(appeal.getCreatedAt())
                .build();
    }
}
