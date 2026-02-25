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
      Pageable pageable);

  @Query("SELECT u FROM User u WHERE u.role.roleName <> :roleName AND u.deletedAt IS NULL")
  java.util.List<User> findAllByRoleNameNot(@Param("roleName") String roleName);

  @Query("SELECT u FROM User u " +
      "LEFT JOIN Student s ON s.user = u " +
      "LEFT JOIN Teacher t ON t.user = u " +
      "WHERE u.deletedAt IS NULL " +
      "AND (:roleName IS NULL OR u.role.roleName = :roleName) " +
      "AND (:departmentId IS NULL OR s.department.departmentId = :departmentId OR t.department.departmentId = :departmentId) "
      +
      "AND (:classCode IS NULL OR s.manageClass = :classCode)")
  java.util.List<User> findUsersByCriteria(
      @Param("roleName") String roleName,
      @Param("departmentId") Long departmentId,
      @Param("classCode") String classCode);
}
