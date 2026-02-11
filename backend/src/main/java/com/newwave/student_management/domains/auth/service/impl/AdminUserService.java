package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminUserDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListItemResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.Role;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.repository.RoleRepository;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.auth.service.IAdminUserService;
import com.newwave.student_management.domains.auth.service.ITokenRedisService;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.entity.Student;
import com.newwave.student_management.domains.profile.entity.Teacher;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import com.newwave.student_management.domains.profile.repository.StudentRepository;
import com.newwave.student_management.domains.profile.repository.TeacherRepository;
import com.newwave.student_management.infrastructure.mail.IMailService;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService implements IAdminUserService {

    private static final int ROLE_TEACHER = 2;
    private static final int ROLE_STUDENT = 3;
    private static final int ACTIVATION_TTL_SECONDS = 72 * 3600; // 72 hours
    private static final int PASSWORD_LENGTH = 12;
    private static final String PASSWORD_CHARS_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PASSWORD_CHARS_LOWER = "abcdefghjkmnpqrstuvwxyz";
    private static final String PASSWORD_CHARS_DIGIT = "23456789";
    private static final String PASSWORD_CHARS_SPECIAL = "!@#$%&*";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ITokenRedisService tokenRedisService;
    private final IMailService mailService;
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

    @Override
    @Transactional
    public AdminUserDetailResponse createUser(AdminCreateUserRequest request) {
        String roleName = request.getRole() != null ? request.getRole().trim().toUpperCase() : "";
        if (!"TEACHER".equals(roleName) && !"STUDENT".equals(roleName)) {
            throw new AppException(ErrorCode.INVALID_ROLE);
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (email.isBlank()) {
            throw new AppException(ErrorCode.EMAIL_REQUIRED);
        }
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        String firstName = request.getFirstName() != null ? request.getFirstName().trim() : "";
        String lastName = request.getLastName() != null ? request.getLastName().trim() : "";
        if (firstName.isBlank() || lastName.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "First name and last name are required");
        }

        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE));

        if ("TEACHER".equals(roleName)) {
            String code = request.getTeacherCode() != null ? request.getTeacherCode().trim() : "";
            if (code.isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Teacher code is required");
            }
            if (teacherRepository.existsByTeacherCode(code)) {
                throw new AppException(ErrorCode.TEACHER_CODE_EXISTED);
            }
        } else {
            String code = request.getStudentCode() != null ? request.getStudentCode().trim() : "";
            if (code.isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Student code is required");
            }
            if (studentRepository.existsByStudentCode(code)) {
                throw new AppException(ErrorCode.STUDENT_CODE_EXISTED);
            }
        }

        String plainPassword = generateRandomPassword();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        user = userRepository.save(user);
        UUID userId = user.getUserId();

        if ("TEACHER".equals(roleName)) {
            Teacher teacher = new Teacher();
            teacher.setUser(user);
            teacher.setDepartment(department);
            teacher.setTeacherCode(request.getTeacherCode().trim());
            teacher.setFirstName(firstName);
            teacher.setLastName(lastName);
            teacher.setEmail(email);
            teacher.setPhone(trimToNull(request.getPhone(), 20));
            teacher.setSpecialization(trimToNull(request.getSpecialization(), 100));
            teacher.setAcademicRank(trimToNull(request.getAcademicRank(), 50));
            teacher.setOfficeRoom(trimToNull(request.getOfficeRoom(), 50));
            teacher.setDegreesQualification(request.getDegreesQualification());
            teacherRepository.save(teacher);
        } else {
            Student student = new Student();
            student.setUser(user);
            student.setDepartment(department);
            student.setStudentCode(request.getStudentCode().trim());
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setDob(request.getDob());
            student.setGender(request.getGender());
            student.setMajor(trimToNull(request.getMajor(), 100));
            student.setPhone(trimToNull(request.getPhone(), 20));
            student.setAddress(trimToNull(request.getAddress(), 255));
            student.setYear(request.getYear());
            student.setManageClass(trimToNull(request.getManageClass(), 50));
            student.setGpa(null);
            studentRepository.save(student);
        }

        String activationToken = tokenRedisService.createActivationToken(userId, ACTIVATION_TTL_SECONDS);
        mailService.sendWelcomeEmail(email, firstName, lastName, plainPassword, activationToken);

        return getById(userId);
    }

    private static String generateRandomPassword() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        sb.append(PASSWORD_CHARS_UPPER.charAt(r.nextInt(PASSWORD_CHARS_UPPER.length())));
        sb.append(PASSWORD_CHARS_LOWER.charAt(r.nextInt(PASSWORD_CHARS_LOWER.length())));
        sb.append(PASSWORD_CHARS_DIGIT.charAt(r.nextInt(PASSWORD_CHARS_DIGIT.length())));
        sb.append(PASSWORD_CHARS_SPECIAL.charAt(r.nextInt(PASSWORD_CHARS_SPECIAL.length())));
        String all = PASSWORD_CHARS_UPPER + PASSWORD_CHARS_LOWER + PASSWORD_CHARS_DIGIT + PASSWORD_CHARS_SPECIAL;
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            sb.append(all.charAt(r.nextInt(all.length())));
        }
        // Shuffle
        char[] a = sb.toString().toCharArray();
        for (int i = a.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            char t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
        return new String(a);
    }

    private static String trimToNull(String s, int maxLen) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
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


