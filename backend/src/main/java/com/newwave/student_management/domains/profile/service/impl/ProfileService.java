package com.newwave.student_management.domains.profile.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.profile.dto.request.UpdateProfileRequest;
import com.newwave.student_management.domains.profile.dto.request.UpdateStudentProfileRequest;
import com.newwave.student_management.domains.profile.dto.request.UpdateTeacherProfileRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService implements IProfileService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SemesterRepository semesterRepository;

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
}
