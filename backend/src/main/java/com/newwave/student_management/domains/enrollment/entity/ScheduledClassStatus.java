package com.newwave.student_management.domains.enrollment.entity;

/**
 * Trạng thái lớp học phần.
 *
 * <ul>
 *   <li>OPEN     — Đang mở, sinh viên có thể đăng ký</li>
 *   <li>CLOSED   — Đóng đăng ký (đủ sĩ số hoặc admin khoá)</li>
 *   <li>CANCELLED — Lớp bị hủy (không xóa vật lý để giữ enrollment history)</li>
 * </ul>
 */
public enum ScheduledClassStatus {
    OPEN,
    CLOSED,
    CANCELLED
}
