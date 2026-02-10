package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.domains.auth.dto.response.AdminUserListItemResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminUserListResponse;
import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import com.newwave.student_management.domains.auth.repository.UserRepository;
import com.newwave.student_management.domains.auth.service.IAdminUserService;
import com.newwave.student_management.infrastructure.storage.IStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService implements IAdminUserService {

    private final UserRepository userRepository;
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
}


