package com.newwave.student_management.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // System errors (9xxx)
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9998, "Invalid error key", HttpStatus.BAD_REQUEST),

    // Validation errors - Generic (1000-1099)
    VALIDATION_ERROR(1000, "Validation error", HttpStatus.BAD_REQUEST),

    // Validation errors - Email (1100-1119)
    EMAIL_REQUIRED(1100, "Email is required", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(1101, "Email is invalid. Must be a valid @fpt.edu.vn email", HttpStatus.BAD_REQUEST),
    EMAIL_FORMAT_INVALID(1102, "Email format is invalid",
            HttpStatus.BAD_REQUEST),
    EMAIL_TOO_LONG(1103, "Email must be at most 255 characters", HttpStatus.BAD_REQUEST),

    // Validation errors - Password (1120-1139)
    PASSWORD_REQUIRED(1120, "Password is required", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1121, "Password must be between 8 and 100 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_WEAK(1122, "Password must contain at least one uppercase letter, one lowercase letter, and one digit",
            HttpStatus.BAD_REQUEST),


    // Validation errors - Token (1180-1199)
    TOKEN_REQUIRED(1180, "Token is required", HttpStatus.BAD_REQUEST),
    TOKEN_INVALID(1181, "Token is invalid", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(1182, "Token has expired", HttpStatus.UNAUTHORIZED),

    // Business errors - User (1200-1299)
    USER_EXISTED(1200, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_EXISTED(1201, "User not found", HttpStatus.NOT_FOUND),
    USER_CREATION_FAILED(1202, "Failed to create user profile", HttpStatus.INTERNAL_SERVER_ERROR),
    TEACHER_CODE_EXISTED(1203, "Teacher code already exists", HttpStatus.CONFLICT),
    STUDENT_CODE_EXISTED(1204, "Student code already exists", HttpStatus.CONFLICT),
    INVALID_ROLE(1210, "Invalid role (must be TEACHER or STUDENT)", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NOT_FOUND(1220, "Department not found", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NAME_REQUIRED(1221, "Department name is required", HttpStatus.BAD_REQUEST),
    DEPARTMENT_NAME_EXISTED(1222, "Department name already exists", HttpStatus.CONFLICT),

    // Business errors - Address (1250-1269)
    ADDRESS_NOT_FOUND(1250, "Address not found", HttpStatus.NOT_FOUND),
    ADDRESS_LIMIT_EXCEEDED(1251, "Maximum number of addresses reached", HttpStatus.BAD_REQUEST),

    // Authentication errors (1300-1399)
    UNAUTHENTICATED(1300, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1301, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(1302, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_ACTIVE(1303, "Account is not active. Please verify your email or contact support.",
            HttpStatus.FORBIDDEN),
    ACCOUNT_BLOCKED(1304, "Your account has been blocked. Please contact support.", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED(1305, "Please verify your email before logging in", HttpStatus.FORBIDDEN),
    RATE_LIMITED(1306, "Too many login attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS),
    EMAIL_ALREADY_VERIFIED(1307, "Email is already verified", HttpStatus.BAD_REQUEST),
    EMAIL_RESEND_COOLDOWN(1308, "Please wait before resending verification email", HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_RESET_COOLDOWN(1309, "Please wait before requesting password reset again",
            HttpStatus.TOO_MANY_REQUESTS),
    PASSWORD_MISMATCH(1310, "New password and confirm password do not match", HttpStatus.BAD_REQUEST),
    INVALID_RESET_TOKEN(1311, "Invalid or expired password reset token", HttpStatus.BAD_REQUEST),
    ACTIVATION_TOKEN_INVALID(1314, "Invalid or expired activation link. Please request a new one.", HttpStatus.BAD_REQUEST),
    INCORRECT_PASSWORD(1312, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    SAME_PASSWORD(1313, "New password must be different from current password", HttpStatus.BAD_REQUEST),

    // Profile errors (1400-1499)
    STUDENT_ROLE_REQUIRED(1401, "Access denied - Student role required", HttpStatus.FORBIDDEN),
    TEACHER_ROLE_REQUIRED(1402, "Access denied - Teacher role required", HttpStatus.FORBIDDEN),

    // Profile not found errors (1500-1599)
    STUDENT_PROFILE_NOT_FOUND(1501, "Student profile not found", HttpStatus.NOT_FOUND),
    TEACHER_PROFILE_NOT_FOUND(1502, "Teacher profile not found", HttpStatus.NOT_FOUND),

    // File upload errors (1600-1699)
    FILE_REQUIRED(1600, "File is required", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(1601, "File size exceeds maximum limit (5MB)", HttpStatus.BAD_REQUEST),
    FILE_INVALID_FORMAT(1602, "Invalid file format. Allowed: JPG, PNG, WebP", HttpStatus.BAD_REQUEST),
    FILE_PROCESSING_FAILED(1603, "Failed to process image", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_UPLOAD_FAILED(1604, "Failed to upload file to storage", HttpStatus.INTERNAL_SERVER_ERROR),
    ;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;
}