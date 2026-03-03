package com.newwave.student_management.domains.profile.entity;

/**
 * Trạng thái đăng ký tín chỉ của một Semester.
 *
 * DRAFT — Mặc định. Admin đang chuẩn bị lớp, chưa mở đăng ký.
 * PUBLISHED — Admin đã publish: dữ liệu lớp đã được sync lên Redis,
 * sinh viên có thể đăng ký tín chỉ.
 * CLOSED — Đã đóng cổng đăng ký. Dữ liệu Redis có thể bị xóa.
 */
public enum EnrollmentStatus {
    DRAFT,
    PUBLISHED,
    CLOSED
}
