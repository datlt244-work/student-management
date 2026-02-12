package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateUserProfileRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateUserStatusRequest;
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
        String normalizedSearch = PaginationUtil.normalizeSearch(search);

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

                    String fullName = null;
                    int rId = user.getRole().getRoleId();
                    UUID uid = user.getUserId();
                    if (rId == ROLE_TEACHER) {
                        fullName = teacherRepository.findByUser_UserIdAndDeletedAtIsNull(uid)
                                .map(t -> (t.getFirstName() + " " + t.getLastName()).trim())
                                .orElse(null);
                    } else if (rId == ROLE_STUDENT) {
                        fullName = studentRepository.findByUser_UserIdAndDeletedAtIsNull(uid)
                                .map(s -> (s.getFirstName() + " " + s.getLastName()).trim())
                                .orElse(null);
                    }

                    if (fullName == null || fullName.isBlank()) {
                        fullName = buildDisplayNameFromEmail(user.getEmail());
                    }

                    return AdminUserListItemResponse.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .fullName(fullName)
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

        PaginationUtil.PaginationMetadata metadata = PaginationUtil.extractMetadata(pageResult);

        return AdminUserListResponse.builder()
                .content(items)
                .page(metadata.page)
                .size(metadata.size)
                .totalElements(metadata.totalElements)
                .totalPages(metadata.totalPages)
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
            if (teacherRepository.existsByTeacherCodeAndDeletedAtIsNull(code)) {
                throw new AppException(ErrorCode.TEACHER_CODE_EXISTED);
            }
        } else {
            String code = request.getStudentCode() != null ? request.getStudentCode().trim() : "";
            if (code.isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Student code is required");
            }
            if (studentRepository.existsByStudentCodeAndDeletedAtIsNull(code)) {
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

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserProfile(UUID userId, AdminUpdateUserProfileRequest request) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String roleName = user.getRole() != null && user.getRole().getRoleName() != null
                ? user.getRole().getRoleName().trim().toUpperCase()
                : "";

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        }

        if ("TEACHER".equals(roleName)) {
            Teacher teacher = teacherRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.TEACHER_PROFILE_NOT_FOUND));

            if (department != null) {
                teacher.setDepartment(department);
            }

            if (request.getFirstName() != null) {
                teacher.setFirstName(trimRequired(request.getFirstName(), 50, "First name must not be blank"));
            }
            if (request.getLastName() != null) {
                teacher.setLastName(trimRequired(request.getLastName(), 50, "Last name must not be blank"));
            }

            if (request.getTeacherCode() != null) {
                String newCode = trimRequired(request.getTeacherCode(), 10, "Teacher code must not be blank");
                if (teacher.getTeacherCode() == null || !teacher.getTeacherCode().equals(newCode)) {
                    if (teacherRepository.existsByTeacherCodeAndDeletedAtIsNull(newCode)) {
                        throw new AppException(ErrorCode.TEACHER_CODE_EXISTED);
                    }
                }
                teacher.setTeacherCode(newCode);
            }

            if (request.getPhone() != null) {
                teacher.setPhone(trimToNull(request.getPhone(), 20));
            }
            if (request.getSpecialization() != null) {
                teacher.setSpecialization(trimToNull(request.getSpecialization(), 100));
            }
            if (request.getAcademicRank() != null) {
                teacher.setAcademicRank(trimToNull(request.getAcademicRank(), 50));
            }
            if (request.getOfficeRoom() != null) {
                teacher.setOfficeRoom(trimToNull(request.getOfficeRoom(), 50));
            }
            if (request.getDegreesQualification() != null) {
                String dq = request.getDegreesQualification();
                teacher.setDegreesQualification(dq == null || dq.isBlank() ? null : dq.trim());
            }

            teacherRepository.save(teacher);
            return getById(userId);
        }

        if ("STUDENT".equals(roleName)) {
            Student student = studentRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.STUDENT_PROFILE_NOT_FOUND));

            if (department != null) {
                student.setDepartment(department);
            }

            if (request.getFirstName() != null) {
                student.setFirstName(trimRequired(request.getFirstName(), 50, "First name must not be blank"));
            }
            if (request.getLastName() != null) {
                student.setLastName(trimRequired(request.getLastName(), 50, "Last name must not be blank"));
            }

            if (request.getStudentCode() != null) {
                String newCode = trimRequired(request.getStudentCode(), 10, "Student code must not be blank");
                if (student.getStudentCode() == null || !student.getStudentCode().equals(newCode)) {
                    if (studentRepository.existsByStudentCodeAndDeletedAtIsNull(newCode)) {
                        throw new AppException(ErrorCode.STUDENT_CODE_EXISTED);
                    }
                }
                student.setStudentCode(newCode);
            }

            if (request.getDob() != null) {
                student.setDob(request.getDob());
            }
            if (request.getGender() != null) {
                String g = request.getGender();
                student.setGender(g == null || g.isBlank() ? null : g.trim().toUpperCase());
            }
            if (request.getMajor() != null) {
                student.setMajor(trimToNull(request.getMajor(), 100));
            }
            if (request.getPhone() != null) {
                student.setPhone(trimToNull(request.getPhone(), 20));
            }
            if (request.getAddress() != null) {
                student.setAddress(trimToNull(request.getAddress(), 255));
            }
            if (request.getYear() != null) {
                student.setYear(request.getYear());
            }
            if (request.getManageClass() != null) {
                student.setManageClass(trimToNull(request.getManageClass(), 50));
            }

            studentRepository.save(student);
            return getById(userId);
        }

        throw new AppException(ErrorCode.INVALID_ROLE);
    }

    @Override
    @Transactional
    public AdminUserDetailResponse updateUserStatus(UUID userId, AdminUpdateUserStatusRequest request) {
        User user = userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserStatus current = user.getStatus();
        UserStatus target = request.getStatus();

        if (target == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Status is required");
        }

        // Validate transition theo state machine trong UC-11.5
        if (current == UserStatus.PENDING_VERIFICATION && target == UserStatus.BLOCKED) {
            // Cho phép block luôn từ pending
        } else if (current == UserStatus.PENDING_VERIFICATION && target == UserStatus.INACTIVE) {
            // Cho phép đặt INACTIVE nếu cần
        } else if (current == UserStatus.PENDING_VERIFICATION && target == UserStatus.ACTIVE) {
            // Thông thường ACTIVE sẽ qua flow activate, nhưng cho phép admin override
        }

        if (target == UserStatus.BLOCKED) {
            String reason = request.getBanReason() != null ? request.getBanReason().trim() : "";
            if (reason.isBlank()) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "Ban reason is required when blocking user");
            }
            user.setStatus(UserStatus.BLOCKED);
            user.setBanReason(reason);

            // Force logout: invalidate all tokens
            tokenRedisService.deleteAllRefreshTokensForUser(userId);
            tokenRedisService.incrementTokenVersion(userId);
        } else if (target == UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            user.setBanReason(null);
        } else if (target == UserStatus.INACTIVE) {
            user.setStatus(UserStatus.INACTIVE);
            // Optional banReason when inactive
            String reason = request.getBanReason();
            user.setBanReason(reason == null || reason.isBlank() ? null : reason.trim());

            tokenRedisService.deleteAllRefreshTokensForUser(userId);
            tokenRedisService.incrementTokenVersion(userId);
        } else if (target == UserStatus.PENDING_VERIFICATION) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Cannot set status back to PENDING_VERIFICATION");
        }

        userRepository.save(user);
        return getById(userId);
    }

    @Override
    @Transactional
    public void softDeleteUser(UUID targetUserId, UUID actingAdminId) {
        if (targetUserId.equals(actingAdminId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Admin cannot delete own account");
        }

        User user = userRepository.findByUserIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Soft delete user
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        // Soft delete related profiles if exist
        teacherRepository.findByUser_UserIdAndDeletedAtIsNull(targetUserId).ifPresent(t -> {
            t.setDeletedAt(java.time.LocalDateTime.now());
            teacherRepository.save(t);
        });
        studentRepository.findByUser_UserIdAndDeletedAtIsNull(targetUserId).ifPresent(s -> {
            s.setDeletedAt(java.time.LocalDateTime.now());
            studentRepository.save(s);
        });

        // Force logout: invalidate tokens
        tokenRedisService.deleteAllRefreshTokensForUser(targetUserId);
        tokenRedisService.incrementTokenVersion(targetUserId);
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

    private static String buildDisplayNameFromEmail(String email) {
        if (email == null || email.isBlank()) return "";
        String local = email.split("@", 2)[0];
        if (local == null || local.isBlank()) return email;
        String[] parts = local.split("[._]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0)))
                    .append(p.substring(1));
        }
        return sb.length() > 0 ? sb.toString() : email;
    }

    private static String trimRequired(String s, int maxLen, String errorMessage) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, errorMessage);
        }
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


