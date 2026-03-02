package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.domains.auth.service.IAdminUserImportService;
import com.newwave.student_management.domains.facility.entity.Room;
import com.newwave.student_management.domains.facility.repository.RoomRepository;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.entity.DepartmentStatus;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserImportServiceImpl implements IAdminUserImportService {

    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final com.newwave.student_management.domains.auth.repository.ImportJobRepository importJobRepository;
    private final com.newwave.student_management.domains.auth.service.UserImportProducer userImportProducer;
    private final com.newwave.student_management.domains.auth.repository.UserRepository userRepository;

    @Override
    public byte[] generateTeacherTemplate() {
        return generateTemplate(new String[] {
                "Email (*@fpt.edu.vn)", "Department ID", "First Name", "Last Name", "Phone",
                "Teacher Code (HJxxxxxx)", "Specialization", "Academic Rank", "Office Room", "Degrees / Qualification"
        }, getActiveDepartments(), getActiveRooms());
    }

    @Override
    public byte[] generateStudentTemplate() {
        return generateTemplate(new String[] {
                "Email (*@fpt.edu.vn)", "Department ID", "First Name", "Last Name", "Phone",
                "Student Code (HExxxxxx)", "Date of Birth (yyyy-MM-dd)", "Gender (MALE/FEMALE/OTHER)", "Major",
                "Year (1-4)", "Manage Class"
        }, getActiveDepartments(), null);
    }

    private List<Department> getActiveDepartments() {
        return departmentRepository.findAllByStatusAndDeletedAtIsNull(DepartmentStatus.ACTIVE);
    }

    private List<Room> getActiveRooms() {
        return roomRepository.findAllByDeletedAtIsNull();
    }

    private byte[] generateTemplate(String[] headers, List<Department> departments, List<Room> rooms) {
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

            // Sheet 3: Reference Data - Rooms (only for Teacher template)
            if (rooms != null && !rooms.isEmpty()) {
                Sheet sheet3 = workbook.createSheet("Reference Data - Rooms");
                Row headerRow3 = sheet3.createRow(0);
                String[] roomHeaders = { "Room Name", "Room Type", "Capacity", "Assigned Teacher", "Status" };
                for (int i = 0; i < roomHeaders.length; i++) {
                    Cell cell = headerRow3.createCell(i);
                    cell.setCellValue(roomHeaders[i]);
                    cell.setCellStyle(headerStyle);
                    sheet3.setColumnWidth(i, 6000);
                }

                // Styling for occupied rooms (red background)
                CellStyle occupiedStyle = workbook.createCellStyle();
                Font occupiedFont = workbook.createFont();
                occupiedFont.setColor(IndexedColors.RED.getIndex());
                occupiedStyle.setFont(occupiedFont);
                occupiedStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
                occupiedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // Styling for available rooms (green background)
                CellStyle availableStyle = workbook.createCellStyle();
                Font availableFont = workbook.createFont();
                availableFont.setColor(IndexedColors.DARK_GREEN.getIndex());
                availableStyle.setFont(availableFont);
                availableStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                availableStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                int roomRowIdx = 1;
                for (Room room : rooms) {
                    Row row = sheet3.createRow(roomRowIdx++);
                    boolean isOccupied = room.getAssignedTeacher() != null;
                    CellStyle rowStyle = isOccupied ? occupiedStyle : availableStyle;

                    Cell nameCell = row.createCell(0);
                    nameCell.setCellValue(room.getName());
                    nameCell.setCellStyle(rowStyle);

                    Cell typeCell = row.createCell(1);
                    typeCell.setCellValue(room.getRoomType() != null ? room.getRoomType().name() : "");
                    typeCell.setCellStyle(rowStyle);

                    Cell capCell = row.createCell(2);
                    capCell.setCellValue(room.getCapacity() != null ? room.getCapacity() : 0);
                    capCell.setCellStyle(rowStyle);

                    Cell teacherCell = row.createCell(3);
                    if (isOccupied) {
                        teacherCell.setCellValue(room.getAssignedTeacher().getFirstName() + " "
                                + room.getAssignedTeacher().getLastName());
                    } else {
                        teacherCell.setCellValue("");
                    }
                    teacherCell.setCellStyle(rowStyle);

                    Cell statusCell = row.createCell(4);
                    statusCell.setCellValue(isOccupied ? "OCCUPIED" : "AVAILABLE");
                    statusCell.setCellStyle(rowStyle);
                }

                sheet3.protectSheet("admin@123");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Error generating Excel template", e);
            throw new AppException(ErrorCode.EXCEL_PROCESSING_ERROR);
        }
    }

    @Override
    public void triggerBatchImport(org.springframework.web.multipart.MultipartFile file, String role) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || (!originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls"))) {
            throw new AppException(ErrorCode.EXCEL_FILE_INVALID_FORMAT);
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = 0;
            java.util.List<Row> validRows = new java.util.ArrayList<>();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null && isRowNotEmpty(row)) {
                    totalRows++;
                    validRows.add(row);
                }
            }

            if (totalRows > 2000) {
                throw new AppException(ErrorCode.EXCEL_FILE_TOO_LARGE_ROWS);
            }
            if (totalRows == 0) {
                throw new AppException(ErrorCode.EXCEL_FILE_EMPTY);
            }

            String userEmail = org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
            com.newwave.student_management.domains.auth.entity.User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            com.newwave.student_management.domains.auth.entity.ImportJob importJob = com.newwave.student_management.domains.auth.entity.ImportJob
                    .builder()
                    .status("IN_PROGRESS")
                    .role(role)
                    .totalRows(totalRows)
                    .fileName(originalFilename)
                    .createdBy(currentUser)
                    .build();

            importJob = importJobRepository.save(importJob);

            for (Row row : validRows) {
                com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest payload = parseRowToPayload(
                        row, role);

                com.newwave.student_management.domains.auth.event.UserImportEvent event = com.newwave.student_management.domains.auth.event.UserImportEvent
                        .builder()
                        .jobId(importJob.getId())
                        .rowNumber(row.getRowNum() + 1)
                        .totalRows(totalRows)
                        .requestPayload(payload)
                        .build();

                userImportProducer.sendImportEvent(event);
            }

        } catch (AppException e) {
            throw e; // re-throw AppException so it's not swallowed by generic Exception catch
        } catch (Exception e) {
            log.error("Failed to read Excel file", e);
            throw new AppException(ErrorCode.EXCEL_PROCESSING_ERROR);
        }
    }

    private boolean isRowNotEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return true;
            }
        }
        return false;
    }

    private String getCellValue(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null)
            return null;
        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest parseRowToPayload(Row row,
            String role) {
        com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest payload = new com.newwave.student_management.domains.auth.dto.request.AdminCreateUserRequest();
        payload.setRole(role);

        try {
            payload.setEmail(getCellValue(row, 0));
            String deptStr = getCellValue(row, 1);
            if (deptStr != null) {
                payload.setDepartmentId(Double.valueOf(deptStr.replaceAll("[^\\d.]", "")).intValue());
            }
            payload.setFirstName(getCellValue(row, 2));
            payload.setLastName(getCellValue(row, 3));
            payload.setPhone(getCellValue(row, 4));

            if ("TEACHER".equals(role)) {
                payload.setTeacherCode(getCellValue(row, 5));
                payload.setSpecialization(getCellValue(row, 6));
                payload.setAcademicRank(getCellValue(row, 7));
                payload.setOfficeRoom(getCellValue(row, 8));
                payload.setDegreesQualification(getCellValue(row, 9));
            } else if ("STUDENT".equals(role)) {
                payload.setStudentCode(getCellValue(row, 5));
                String dobStr = getCellValue(row, 6);
                if (dobStr != null) {
                    try {
                        payload.setDob(java.time.LocalDate.parse(dobStr));
                    } catch (Exception ignores) {
                        try {
                            payload.setDob(row.getCell(6).getLocalDateTimeCellValue().toLocalDate());
                        } catch (Exception e) {
                            // Leave it null if invalid, validator will catch
                        }
                    }
                }
                payload.setGender(getCellValue(row, 7));
                payload.setMajor(getCellValue(row, 8));
                String yearStr = getCellValue(row, 9);
                if (yearStr != null) {
                    payload.setYear(Double.valueOf(yearStr.replaceAll("[^\\d.]", "")).intValue());
                }
                payload.setManageClass(getCellValue(row, 10));
            }
        } catch (Exception ex) {
            log.warn("Error parsing row {}: {}", row.getRowNum(), ex.getMessage());
            // Let it pass with null/incorrect values so Consumer handles the validation
            // logic (as requested in UC-12.3)
        }

        return payload;
    }
}
