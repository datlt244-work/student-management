package com.newwave.student_management.domains.enrollment.scheduler;

import com.newwave.student_management.domains.enrollment.service.IAdminClassService;
import com.newwave.student_management.domains.profile.entity.EnrollmentStatus;
import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EnrollmentDeadlineScheduler {

    private final SemesterRepository semesterRepository;
    private final IAdminClassService adminClassService;

    // Checks every minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processEnrollmentDeadlines() {
        log.debug("Running enrollment deadline check...");
        List<Semester> publishedSemesters = semesterRepository.findByEnrollmentStatus(EnrollmentStatus.PUBLISHED);

        LocalDateTime now = LocalDateTime.now();

        for (Semester semester : publishedSemesters) {
            if (semester.getPublishedAt() != null) {
                // The deadline is currently fixed at 72 hours after publish
                LocalDateTime deadline = semester.getPublishedAt().plusHours(72);

                if (now.isAfter(deadline)) {
                    log.info(
                            "Enrollment deadline passed for semester: {}. Closing enrollment and consolidating classes.",
                            semester.getDisplayName());
                    semester.setEnrollmentStatus(EnrollmentStatus.CLOSED);
                    semesterRepository.save(semester);

                    // Call the consolidation logic to cancel under-enrolled classes
                    adminClassService.lockClassesAndConsolidate(semester.getSemesterId());
                }
            }
        }
    }
}
