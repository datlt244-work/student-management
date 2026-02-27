package com.newwave.student_management.domains.auth.controller;

import com.newwave.student_management.domains.auth.service.IAdminUserImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users/import")
@RequiredArgsConstructor
@Tag(name = "Admin - User Import", description = "API liên quan đến import user qua Excel (UC-12)")
public class AdminUserImportController {

        private final IAdminUserImportService adminUserImportService;

        @GetMapping("/templates/teacher")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "UC-12.1 - Tải File Mẫu Import Teacher (Excel)")
        public ResponseEntity<byte[]> downloadTeacherTemplate() {
                byte[] excelBytes = adminUserImportService.generateTeacherTemplate();

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"Template_Teacher.xlsx\"")
                                .contentType(
                                                MediaType.parseMediaType(
                                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(excelBytes);
        }

        @GetMapping("/templates/student")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "UC-12.1 - Tải File Mẫu Import Student (Excel)")
        public ResponseEntity<byte[]> downloadStudentTemplate() {
                byte[] excelBytes = adminUserImportService.generateStudentTemplate();

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"Template_Student.xlsx\"")
                                .contentType(
                                                MediaType.parseMediaType(
                                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(excelBytes);
        }

        @org.springframework.web.bind.annotation.PostMapping("/{role}")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "UC-12.2 - Batch User Import via Excel")
        public ResponseEntity<com.newwave.student_management.common.dto.ApiResponse<String>> importUsers(
                        @org.springframework.web.bind.annotation.RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                        @org.springframework.web.bind.annotation.PathVariable("role") String role) {

                adminUserImportService.triggerBatchImport(file, role.toUpperCase());

                return ResponseEntity.accepted().body(
                                com.newwave.student_management.common.dto.ApiResponse.<String>builder()
                                                .code(1000)
                                                .result("Hệ thống đang xử lý import ở chế độ nền. Bạn sẽ nhận được thông báo khi hoàn tất.")
                                                .build());
        }
}
