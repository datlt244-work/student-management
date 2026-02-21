package com.newwave.student_management.domains.auth.service.impl;

import com.newwave.student_management.domains.profile.entity.Semester;
import com.newwave.student_management.domains.profile.repository.SemesterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemesterAutomationService {

    private final SemesterRepository semesterRepository;

    @PostConstruct
    public void onStartup() {
        log.info("System startup: Triggering initial semester activation check.");
        autoUpdateCurrentSemester();
    }

    /**
     * Tự động cập nhật học kỳ hiện tại dựa trên ngày bắt đầu và kết thúc.
     * Chạy mỗi ngày vào lúc 0:01 AM.
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void autoUpdateCurrentSemester() {
        LocalDate today = LocalDate.now();
        log.info("Starting automatic semester activation check for date: {}", today);

        List<Semester> activeRangeSemesters = semesterRepository.findByDateInRange(today);
        
        if (activeRangeSemesters.isEmpty()) {
            log.info("No semester found matching today's date range. No action taken.");
            return;
        }

        // Ưu tiên học kỳ có ID lớn nhất hoặc năm gần nhất nếu có sự chồng chéo (thường không xảy ra)
        Semester targetSemester = activeRangeSemesters.get(0);
        
        if (targetSemester.isCurrent()) {
            log.info("Semester '{}' is already active. Nothing to change.", targetSemester.getDisplayName());
            return;
        }

        log.info("Activating semester '{}' and deactivating previous current semester.", targetSemester.getDisplayName());

        // Hủy kích hoạt học kỳ hiện tại cũ
        semesterRepository.findByIsCurrentTrue().ifPresent(old -> {
            old.setCurrent(false);
            semesterRepository.save(old);
            log.info("Deactivated old current semester: {}", old.getDisplayName());
        });

        // Kích hoạt học kỳ mới
        targetSemester.setCurrent(true);
        semesterRepository.save(targetSemester);
        
        log.info("Successfully activated semester: {}", targetSemester.getDisplayName());
    }
}
