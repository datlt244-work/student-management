package com.newwave.student_management.domains.notification.repository;

import com.newwave.student_management.domains.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
        Optional<FcmToken> findByToken(String token);

        List<FcmToken> findByUser_UserId(UUID userId);

        void deleteByToken(String token);

        @Query("SELECT t FROM FcmToken t JOIN t.user u WHERE u.role.roleName <> :roleName")
        List<FcmToken> findAllByRoleNot(@Param("roleName") String roleName);

        @Query("SELECT t FROM FcmToken t " +
                        "JOIN t.user u " +
                        "LEFT JOIN Student s ON s.user = u " +
                        "LEFT JOIN Teacher tr ON tr.user = u " +
                        "WHERE (:roleName IS NULL OR u.role.roleName = :roleName) " +
                        "AND (:departmentId IS NULL OR s.department.departmentId = :departmentId OR tr.department.departmentId = :departmentId) "
                        +
                        "AND (:classCode IS NULL OR s.manageClass = :classCode)")
        List<FcmToken> findTokensByCriteria(
                        @Param("roleName") String roleName,
                        @Param("departmentId") Long departmentId,
                        @Param("classCode") String classCode);
}
