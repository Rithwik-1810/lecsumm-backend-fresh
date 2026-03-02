package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.Lecture;
import com.cvr.cse.lecturesummarizer.models.Summary;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.LectureRepository;
import com.cvr.cse.lecturesummarizer.repositories.SummaryRepository;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SummaryService {

    private static final Logger logger = LoggerFactory.getLogger(SummaryService.class);

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LectureRepository lectureRepository;

    public List<Summary> getUserSummaries(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            return summaryRepository.findByUserIdOrderByCreatedAtDesc(userOpt.get().getId());
        }
        throw new RuntimeException("User not found");
    }

    public Summary getSummary(String id) {
        return summaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Summary not found"));
    }

    public Summary toggleSaveSummary(String id) {
        Summary summary = getSummary(id);
        summary.setSaved(!summary.isSaved());
        return summaryRepository.save(summary);
    }

    public void deleteSummary(String id) {
        Summary summary = getSummary(id);
        String userId = summary.getUserId();
        String lectureId = summary.getLectureId();

        logger.info("Deleting summary {} for user {}, lecture {}", id, userId, lectureId);

        // Delete the summary
        summaryRepository.deleteById(id);
        logger.info("Summary {} deleted", id);

        // Update user stats
        userRepository.findById(userId).ifPresent(user -> {
            User.UserStats stats = user.getStats();
            int oldTotal = stats.getTotalSummaries();
            stats.setTotalSummaries(Math.max(0, oldTotal - 1));
            logger.info("Decremented totalSummaries from {} to {}", oldTotal, stats.getTotalSummaries());

            // Subtract the lecture's duration from hoursSaved
            if (lectureId != null) {
                lectureRepository.findById(lectureId).ifPresentOrElse(lecture -> {
                    double durationHours = lecture.getDurationSeconds() / 3600.0;
                    double oldHours = stats.getHoursSaved();
                    double newHours = oldHours - durationHours;
                    // Prevent negative hours due to floating point inaccuracies
                    if (newHours < 0 && newHours > -0.0001) newHours = 0;
                    stats.setHoursSaved(Math.max(0, newHours));
                    logger.info("Subtracted {} hours from user {} (was {}, now {})",
                            durationHours, userId, oldHours, stats.getHoursSaved());
                }, () -> {
                    logger.warn("Lecture {} not found – cannot subtract hours", lectureId);
                });
            } else {
                logger.warn("Summary {} has no lectureId – cannot subtract hours", id);
            }

            userRepository.save(user);
            logger.info("User stats updated after summary deletion for user {}", userId);
        });
    }
}