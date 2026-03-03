package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.Lecture;
import com.cvr.cse.lecturesummarizer.models.Summary;
import com.cvr.cse.lecturesummarizer.models.Task;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.LectureRepository;
import com.cvr.cse.lecturesummarizer.repositories.SummaryRepository;
import com.cvr.cse.lecturesummarizer.repositories.TaskRepository;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessor.class);

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AIService aiService;

    @Async
    public void processLecture(Lecture lecture, String filePath) {
        System.out.println(">>> ASYNC PROCESSING STARTED for lecture: " + lecture.getId());
        logger.info(">>> ASYNC PROCESSING STARTED (logger) for lecture: {}", lecture.getId());

        try {
            lecture.setStatus("processing");
            lecture.setUpdatedAt(java.time.LocalDateTime.now());
            lectureRepository.save(lecture);
            logger.info("Processing lecture: {} (file: {})", lecture.getId(), filePath);

            AIService.AIResponse aiResponse = aiService.processLecture(
                filePath,
                lecture.getLanguage(),
                lecture.isExtractTasks(),
                lecture.isGenerateSummary()
            );

            logger.info("AI response received. Duration: {} seconds", aiResponse.getDurationSeconds());

            lecture.setDurationSeconds(aiResponse.getDurationSeconds());
            lectureRepository.save(lecture);

            if (aiResponse != null && aiResponse.getSummary() != null) {
                Summary summary = new Summary();
                summary.setLectureId(lecture.getId());
                summary.setUserId(lecture.getUserId());
                summary.setContent(aiResponse.getSummary().getContent());
                summary.setKeyPoints(aiResponse.getSummary().getKeyPoints());
                summary.setTopics(aiResponse.getSummary().getTopics());
                summary.setTranscript(aiResponse.getTranscript());
                summary.setConfidence(aiResponse.getSummary().getConfidence());
                summary.setCreatedAt(java.time.LocalDateTime.now());

                Summary savedSummary = summaryRepository.save(summary);
                logger.info("Summary saved with ID: {} for lecture: {}", savedSummary.getId(), lecture.getId());

                userRepository.findById(lecture.getUserId()).ifPresent(user -> {
                    user.getStats().setTotalSummaries(user.getStats().getTotalSummaries() + 1);
                    double hoursSaved = aiResponse.getDurationSeconds() / 3600.0;
                    user.getStats().setHoursSaved(user.getStats().getHoursSaved() + hoursSaved);
                    userRepository.save(user);
                    logger.info("User stats updated for user: {} (added {} hours, new total {})",
                            user.getId(), hoursSaved, user.getStats().getHoursSaved());
                });
            } else {
                logger.warn("No summary in AI response for lecture: {}", lecture.getId());
            }

            if (aiResponse != null && aiResponse.getTasks() != null && !aiResponse.getTasks().isEmpty()) {
                for (AIService.TaskDTO taskDTO : aiResponse.getTasks()) {
                    Task task = new Task();
                    task.setUserId(lecture.getUserId());
                    task.setLectureId(lecture.getId());
                    task.setTitle(taskDTO.getTitle());
                    task.setDescription(taskDTO.getDescription());
                    task.setPriority(taskDTO.getPriority());
                    task.setStatus("pending");
                    task.setProgress(0);
                    task.setCreatedAt(java.time.LocalDateTime.now());
                    task.setUpdatedAt(java.time.LocalDateTime.now());

                    Task savedTask = taskRepository.save(task);
                    logger.info("Task saved with ID: {} for lecture: {}", savedTask.getId(), lecture.getId());
                }
            } else {
                logger.info("No tasks to save for lecture: {}", lecture.getId());
            }

            lecture.setStatus("completed");
            lecture.setUpdatedAt(java.time.LocalDateTime.now());
            lectureRepository.save(lecture);
            logger.info("Lecture processing completed for ID: {}", lecture.getId());

        } catch (Exception e) {
            lecture.setStatus("failed");
            lecture.setUpdatedAt(java.time.LocalDateTime.now());
            lectureRepository.save(lecture);
            logger.error("Failed to process lecture: {}", lecture.getId(), e);
        }
    }
}