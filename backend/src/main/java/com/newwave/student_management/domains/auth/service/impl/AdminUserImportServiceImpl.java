package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.domains.auth.service.IAdminUserImportService;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.entity.DepartmentStatus;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserImportServiceImpl implements IAdminUserImportService {

    private final DepartmentRepository departmentRepository;

    @Override
    public byte[] generateTeacherTemplate() {
        return generateTemplate(new String[] {
                "Email (*@fpt.edu.vn)", "Department ID", "First Name", "Last Name", "Phone",
                "Teacher Code (HJxxxxxx)", "Specialization", "Academic Rank", "Office Room", "Degrees / Qualification"
        }, getActiveDepartments());
    }

    @Override
    public byte[] generateStudentTemplate() {
        return generateTemplate(new String[] {
                "Email (*@fpt.edu.vn)", "Department ID", "First Name", "Last Name", "Phone",
                "Student Code (HExxxxxx)", "Date of Birth (yyyy-MM-dd)", "Gender (MALE/FEMALE/OTHER)", "Major",
                "Year (1-4)", "Manage Class"
        }, getActiveDepartments());
    }

    private List<Department> getActiveDepartments() {
        return departmentRepository.findAllByStatusAndDeletedAtIsNull(DepartmentStatus.ACTIVE);
    }

    private byte[] generateTemplate(String[] headers, List<Department> departments) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet 1: Data Input
            Sheet sheet1 = workbook.createSheet("Data Input");
            Row headerRow1 = sheet1.createRow(0);

            // Styling for header
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow1.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet1.setColumnWidth(i, 6000);
            }

            // Sheet 2: Reference Data - Departments
            Sheet sheet2 = workbook.createSheet("Reference Data - Departments");
            Row headerRow2 = sheet2.createRow(0);
            String[] refHeaders = { "Department ID", "Department Name" };
            for (int i = 0; i < refHeaders.length; i++) {
                Cell cell = headerRow2.createCell(i);
                cell.setCellValue(refHeaders[i]);
                cell.setCellStyle(headerStyle);
                sheet2.setColumnWidth(i, 8000);
            }

            // Populate Sheet 2 with active departments
            int rowIdx = 1;
            for (Department dept : departments) {
                Row row = sheet2.createRow(rowIdx++);
                row.createCell(0).setCellValue(dept.getDepartmentId());
                row.createCell(1).setCellValue(dept.getName());
            }

            // Protect Sheet 2 to prevent accidental editing
            sheet2.protectSheet("admin@123");

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error generating Excel template", e);
            throw new RuntimeException("Could not generate Excel template", e);
        }
    }
}
