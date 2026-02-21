package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.common.exception.AppException;
import com.newwave.student_management.common.exception.ErrorCode;
import com.newwave.student_management.common.util.PaginationUtil;
import com.newwave.student_management.domains.auth.dto.request.AdminCreateCourseRequest;
import com.newwave.student_management.domains.auth.dto.request.AdminUpdateCourseRequest;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseDetailResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListItemResponse;
import com.newwave.student_management.domains.auth.dto.response.AdminCourseListResponse;
import com.newwave.student_management.domains.auth.service.IAdminCourseService;
import com.newwave.student_management.domains.curriculum.entity.Course;
import com.newwave.student_management.domains.curriculum.entity.CourseStatus;
import com.newwave.student_management.domains.curriculum.repository.CourseRepository;
import com.newwave.student_management.domains.profile.entity.Department;
import com.newwave.student_management.domains.profile.entity.DepartmentStatus;
import com.newwave.student_management.domains.profile.repository.DepartmentRepository;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import com.newwave.student_management.domains.profile.entity.Semester;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCourseService implements IAdminCourseService {

    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public AdminCourseListResponse getCourses(String search, CourseStatus status, Integer departmentId, Pageable pageable) {
        String normalizedSearch = PaginationUtil.normalizeSearch(search);
        
        Page<Course> page = courseRepository.searchAdminCourses(normalizedSearch, status, departmentId, pageable);
        
        List<AdminCourseListItemResponse> items = page.getContent().stream()
                .map(this::toListItem)
                .toList();

        PaginationUtil.PaginationMetadata meta = PaginationUtil.extractMetadata(page);

        return AdminCourseListResponse.builder()
                .content(items)
                .page(meta.page)
                .size(meta.size)
                .totalElements(meta.totalElements)
                .totalPages(meta.totalPages)
                .build();
    }

    @Override
    public AdminCourseListItemResponse updateCourseStatus(Integer courseId, CourseStatus status) {
        Course course = courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (status == CourseStatus.ACTIVE 
            && course.getDepartment() != null 
            && course.getDepartment().getStatus() == DepartmentStatus.INACTIVE) {
            throw new AppException(ErrorCode.DEPARTMENT_NOT_ACTIVE);
        }

        course.setStatus(status);
        Course saved = courseRepository.save(course);
        return toListItem(saved);
    }

    @Override
    public AdminCourseDetailResponse getCourseDetail(Integer courseId) {
        Course course = courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        Semester currentSemester = semesterRepository.findByIsCurrentTrue().orElse(null);
        String currentSemesterName = currentSemester != null ? (currentSemester.getDisplayName() != null ? currentSemester.getDisplayName() : currentSemester.getName() + " " + currentSemester.getYear()) : "N/A";

        return AdminCourseDetailResponse.builder()
                .courseId(course.getCourseId())
                .code(course.getCode())
                .name(course.getName())
                .credits(course.getCredits())
                .description(course.getDescription())
                .departmentId(course.getDepartment() != null ? course.getDepartment().getDepartmentId() : null)
                .departmentName(course.getDepartment() != null ? course.getDepartment().getName() : null)
                .status(course.getStatus() != null ? course.getStatus().name() : null)
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .createdBy(course.getCreatedBy())
                .currentSemester(currentSemesterName)
                .build();
    }

    @Override
    public AdminCourseDetailResponse updateCourse(Integer courseId, AdminUpdateCourseRequest request) {
        Course course = courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        course.setName(request.getName());
        course.setCredits(request.getCredits());
        course.setDescription(request.getDescription());

        // Update Code if changed
        if (request.getCode() != null && !request.getCode().equals(course.getCode())) {
            if (courseRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
                throw new AppException(ErrorCode.COURSE_CODE_EXISTED);
            }
            course.setCode(request.getCode());
        }

        // Update Department if changed
        if (request.getDepartmentId() != null) {
            boolean isNewDept = course.getDepartment() == null || !course.getDepartment().getDepartmentId().equals(request.getDepartmentId());
            if (isNewDept) {
                Department department = departmentRepository.findByDepartmentIdAndDeletedAtIsNull(request.getDepartmentId())
                        .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
                course.setDepartment(department);
            }
        }

        courseRepository.save(course);
        return getCourseDetail(courseId);
    }

    @Override
    public AdminCourseDetailResponse createCourse(AdminCreateCourseRequest request) {
        // Validate course code uniqueness
        if (courseRepository.existsByCodeAndDeletedAtIsNull(request.getCode())) {
            throw new AppException(ErrorCode.COURSE_CODE_EXISTED);
        }

        // Validate department
        Department department = departmentRepository.findByDepartmentIdAndDeletedAtIsNull(request.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        if (department.getStatus() == DepartmentStatus.INACTIVE) {
            throw new AppException(ErrorCode.DEPARTMENT_NOT_ACTIVE);
        }

        Course course = new Course();
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setCredits(request.getCredits());
        course.setDescription(request.getDescription());
        course.setDepartment(department);
        course.setStatus(CourseStatus.ACTIVE);

        Course saved = courseRepository.save(course);
        return getCourseDetail(saved.getCourseId());
    }

    @Override
    public void deleteCourse(Integer courseId) {
        Course course = courseRepository.findByCourseIdAndDeletedAtIsNull(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        course.setDeletedAt(LocalDateTime.now());
        courseRepository.save(course);
    }

    private AdminCourseListItemResponse toListItem(Course course) {
        return AdminCourseListItemResponse.builder()
                .courseId(course.getCourseId())
                .code(course.getCode())
                .name(course.getName())
                .credits(course.getCredits())
                .departmentId(course.getDepartment() != null ? course.getDepartment().getDepartmentId() : null)
                .departmentName(course.getDepartment() != null ? course.getDepartment().getName() : null)
                .status(course.getStatus() != null ? course.getStatus().name() : null)
                .createdAt(course.getCreatedAt())
                .build();
    }
}
