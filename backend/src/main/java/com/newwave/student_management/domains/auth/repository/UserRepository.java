package com.newwave.student_management.domains.auth.repository;

import com.newwave.student_management.domains.auth.entity.User;
import com.newwave.student_management.domains.auth.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserIdAndDeletedAtIsNull(UUID userId);

    boolean existsByEmail(String email);

    @Query("""
            SELECT u
            FROM User u
            WHERE u.deletedAt IS NULL
              AND (:search IS NULL OR LOWER(u.email) LIKE :search)
              AND (:status IS NULL OR u.status = :status)
              AND (:roleId IS NULL OR u.role.roleId = :roleId)
            """)
    Page<User> searchAdminUsers(
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("roleId") Integer roleId,
            Pageable pageable
    );

}
