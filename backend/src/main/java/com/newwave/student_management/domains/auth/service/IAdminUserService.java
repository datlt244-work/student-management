package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateUserProfileRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateUserStatusRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminUserDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.profile.dto.response.TeacherSimpleResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IAdminUserService {

    AdminUserListResponse getUsers(String search, UserStatus status, Integer roleId, Pageable pageable);

    AdminUserDetailResponse getById(UUID userId);

    AdminUserDetailResponse createUser(AdminCreateUserRequest request);

    AdminUserDetailResponse updateUserProfile(UUID userId, AdminUpdateUserProfileRequest request);

    AdminUserDetailResponse updateUserStatus(UUID userId, AdminUpdateUserStatusRequest request);

    void softDeleteUser(UUID targetUserId, UUID actingAdminId);

    java.util.List<TeacherSimpleResponse> getTeachersByDepartment(Integer departmentId);
}
