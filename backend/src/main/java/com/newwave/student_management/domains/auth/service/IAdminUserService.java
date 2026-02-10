package com.newwave.student_management.domains.auth.service;

import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import org.springframework.data.domain.Pageable;

public interface IAdminUserService {

    AdminUserListResponse getUsers(String search, UserStatus status, Integer roleId, Pageable pageable);
}


