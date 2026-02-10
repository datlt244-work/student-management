package com.newwave.student_management.domains.profile.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.profile.dto.request.UpdateProfileRequest;
import com.newwave.student_management.domains.profile.dto.request.UpdateStudentProfileRequest;
import com.newwave.student_management.domains.profile.dto.request.UpdateTeacherProfileRequest;
import com.newwave.student_management.domains.profile.dto.response.AvatarUploadResponse;
import com.newwave.student_management.domains.profile.dto.response.CombinedProfileResponse;
import com.newwave.student_management.domains.profile.dto.response.SemesterResponse;
import com.newwave.student_management.domains.profile.dto.response.StudentProfileResponse;
import com.newwave.student_management.domains.profile.dto.response.TeacherProfileResponse;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.entity.Teacher;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.domains.profile.repository.TeacherRepository;
import com.newwave.student_management.domains.profile.service.IProfileService;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService implements IProfileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final int MAX_AVATAR_DIMENSION = 500;

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;
    private final IStorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public CombinedProfileResponse getMyProfile(UUID userId, String role) {
        // 1. Fetch User data
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Build base response from User
        CombinedProfileResponse.CombinedProfileResponseBuilder responseBuilder = CombinedProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .role(user.getRole().getRoleName())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .loginCount(user.getLoginCount())
                .createdAt(user.getCreatedAt());

        // 3. Switch by role to fetch additional profile
        String normalizedRole = role.toLowerCase();

        switch (normalizedRole) {
            case "student" -> {
                Student student = studentRepository.findByUserIdWithDepartment(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

                // Fetch current semester
                SemesterResponse currentSemester = semesterRepository.findByIsCurrentTrue()
                        .map(SemesterResponse::fromEntity)
                        .orElse(null);

                responseBuilder.studentProfile(StudentProfileResponse.fromEntity(student, currentSemester));
                responseBuilder.teacherProfile(null);
            }
            case "teacher" -> {
                Teacher teacher = teacherRepository.findByUserIdWithDepartment(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));
                responseBuilder.teacherProfile(TeacherProfileResponse.fromEntity(teacher));
                responseBuilder.studentProfile(null);
            }
            default -> {
                // ADMIN or other roles: no student/teacher profile
                responseBuilder.studentProfile(null);
                responseBuilder.teacherProfile(null);
            }
        }

        log.info("Profile loaded for user {} with role {}", userId, role);
        return responseBuilder.build();
    }

    @Override
    @Transactional
    public CombinedProfileResponse updateMyProfile(UUID userId, String role, UpdateProfileRequest request) {
        String normalizedRole = role.toLowerCase();

        switch (normalizedRole) {
            case "student" -> {
                // Validate: student không được gửi teacherProfile
                if (request.getTeacherProfile() != null) {
                    throw new AppException(ErrorCode.TEACHER_ROLE_REQUIRED);
                }
                if (request.getStudentProfile() != null) {
                    updateStudentProfile(userId, request.getStudentProfile());
                }
            }
            case "teacher" -> {
                // Validate: teacher không được gửi studentProfile
                if (request.getStudentProfile() != null) {
                    throw new AppException(ErrorCode.STUDENT_ROLE_REQUIRED);
                }
                if (request.getTeacherProfile() != null) {
                    updateTeacherProfile(userId, request.getTeacherProfile());
                }
            }
            default -> {
                // ADMIN: không có student/teacher profile để update
            }
        }

        log.info("Profile updated for user {} with role {}", userId, role);

        // Return updated combined profile (same format as GET /profile/me)
        return getMyProfile(userId, role);
    }

    private void updateStudentProfile(UUID userId, UpdateStudentProfileRequest request) {
        Student student = studentRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

        if (request.getPhone() != null) {
            student.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }

        studentRepository.save(student);
    }

    private void updateTeacherProfile(UUID userId, UpdateTeacherProfileRequest request) {
        Teacher teacher = teacherRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

        if (request.getPhone() != null) {
            teacher.setPhone(request.getPhone());
        }

        teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public AvatarUploadResponse uploadAvatar(UUID userId, MultipartFile file) {
        // 1. Validate file
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_REQUIRED);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.FILE_INVALID_FORMAT);
        }

        // 2. Fetch user + get old avatar path
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        String oldAvatarPath = user.getProfilePictureUrl();

        // 3. Image processing: resize to max 500x500, output as JPEG
        byte[] processedImage;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(file.getInputStream())
                    .size(MAX_AVATAR_DIMENSION, MAX_AVATAR_DIMENSION)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(outputStream);
            processedImage = outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to process avatar image for user {}: {}", userId, e.getMessage());
            throw new AppException(ErrorCode.FILE_PROCESSING_FAILED);
        }

        // 4. Generate object name and upload to MinIO
        String objectName = "avatar/" + userId + "/" + UUID.randomUUID() + ".jpg";
        String fullUrl;
        try {
            fullUrl = storageService.uploadFile(
                    objectName,
                    new ByteArrayInputStream(processedImage),
                    processedImage.length,
                    "image/jpeg"
            );
        } catch (Exception e) {
            log.error("Failed to upload avatar to MinIO for user {}: {}", userId, e.getMessage());
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 5. Update DB with new avatar relative path
        try {
            user.setProfilePictureUrl(objectName);
            userRepository.save(user);
        } catch (Exception e) {
            // Rollback: delete the newly uploaded file
            log.error("Failed to update DB for avatar, cleaning up MinIO: {}", e.getMessage());
            storageService.deleteFile(objectName);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 6. Delete old avatar from MinIO (non-critical)
        if (oldAvatarPath != null && !oldAvatarPath.isBlank()) {
            storageService.deleteFile(oldAvatarPath);
        }

        log.info("Avatar updated for user {}: {}", userId, objectName);

        return AvatarUploadResponse.builder()
                .profilePictureUrl(objectName)
                .fullUrl(fullUrl)
                .message("Profile picture updated successfully")
                .build();
    }
}
