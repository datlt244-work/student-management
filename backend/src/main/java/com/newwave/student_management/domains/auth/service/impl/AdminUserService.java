package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.dto.response.AdminUserDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListItemResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.auth.service.IAdminUserService;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.entity.Teacher;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.domains.profile.repository.TeacherRepository;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService implements IAdminUserService {

    private static final int ROLE_TEACHER = 2;
    private static final int ROLE_STUDENT = 3;

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final IStorageService storageService;

    @Override
    public AdminUserListResponse getUsers(String search, UserStatus status, Integer roleId, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";

        Page<User> pageResult = userRepository.searchAdminUsers(
                normalizedSearch,
                status,
                roleId,
                pageable
        );

        List<AdminUserListItemResponse> items = pageResult.getContent().stream()
                .map(user -> {
                    String rawAvatar = user.getProfilePictureUrl();
                    String fullAvatarUrl = (rawAvatar != null && !rawAvatar.isBlank())
                            ? storageService.getPublicUrl(rawAvatar)
                            : null;

                    return AdminUserListItemResponse.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .role(AdminUserListItemResponse.RoleSummary.builder()
                                    .roleId(user.getRole().getRoleId())
                                    .roleName(user.getRole().getRoleName())
                                    .build())
                            .status(user.getStatus().name())
                            .emailVerified(user.isEmailVerified())
                            .profilePictureUrl(fullAvatarUrl)
                            .lastLoginAt(user.getLastLoginAt())
                            .loginCount(user.getLoginCount())
                            .createdAt(user.getCreatedAt())
                            .build();
                })
                .toList();

        return AdminUserListResponse.builder()
                .content(items)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Override
    public AdminUserDetailResponse getById(UUID userId) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        AdminUserDetailResponse.StudentProfilePart studentProfilePart = null;
        AdminUserDetailResponse.TeacherProfilePart teacherProfilePart = null;

        int roleId = user.getRole().getRoleId();
        if (roleId == ROLE_TEACHER) {
            teacherProfilePart = teacherRepository.findByUserIdWithDepartment(userId)
                    .map(this::toTeacherProfilePart)
                    .orElse(null);
        } else if (roleId == ROLE_STUDENT) {
            studentProfilePart = studentRepository.findByUserIdWithDepartment(userId)
                    .map(this::toStudentProfilePart)
                    .orElse(null);
        }

        return AdminUserDetailResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(AdminUserDetailResponse.RoleSummary.builder()
                        .roleId(user.getRole().getRoleId())
                        .roleName(user.getRole().getRoleName())
                        .build())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .banReason(user.getBanReason())
                .lastLoginAt(user.getLastLoginAt())
                .loginCount(user.getLoginCount())
                .createdAt(user.getCreatedAt())
                .studentProfile(studentProfilePart)
                .teacherProfile(teacherProfilePart)
                .build();
    }

    private AdminUserDetailResponse.StudentProfilePart toStudentProfilePart(Student s) {
        Department d = s.getDepartment();
        AdminUserDetailResponse.DepartmentSummary dept = d != null
                ? AdminUserDetailResponse.DepartmentSummary.builder()
                .departmentId(d.getDepartmentId())
                .name(d.getName())
                .build()
                : null;
        return AdminUserDetailResponse.StudentProfilePart.builder()
                .studentId(s.getStudentId())
                .studentCode(s.getStudentCode())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .dob(s.getDob())
                .gender(s.getGender())
                .major(s.getMajor())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .gpa(s.getGpa())
                .year(s.getYear())
                .manageClass(s.getManageClass())
                .department(dept)
                .build();
    }

    private AdminUserDetailResponse.TeacherProfilePart toTeacherProfilePart(Teacher t) {
        Department d = t.getDepartment();
        AdminUserDetailResponse.DepartmentSummary dept = d != null
                ? AdminUserDetailResponse.DepartmentSummary.builder()
                .departmentId(d.getDepartmentId())
                .name(d.getName())
                .build()
                : null;
        return AdminUserDetailResponse.TeacherProfilePart.builder()
                .teacherId(t.getTeacherId())
                .teacherCode(t.getTeacherCode())
                .firstName(t.getFirstName())
                .lastName(t.getLastName())
                .email(t.getEmail())
                .phone(t.getPhone())
                .specialization(t.getSpecialization())
                .academicRank(t.getAcademicRank())
                .officeRoom(t.getOfficeRoom())
                .degreesQualification(t.getDegreesQualification())
                .department(dept)
                .build();
    }
}


