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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LectureService {

    private static final Logger logger = LoggerFactory.getLogger(LectureService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

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

    public Lecture uploadLecture(String email, MultipartFile file, String title,
                                 String language, boolean extractTasks, boolean generateSummary) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath);

        Lecture lecture = new Lecture();
        lecture.setUserId(user.getId());
        lecture.setTitle(title != null ? title : file.getOriginalFilename());
        lecture.setFileName(fileName);
        lecture.setFileUrl("/uploads/" + fileName);
        lecture.setFileSize(file.getSize());

        String contentType = file.getContentType();
        lecture.setFileType(contentType != null && contentType.startsWith("video") ? "video" : "audio");

        lecture.setLanguage(language);
        lecture.setExtractTasks(extractTasks);
        lecture.setGenerateSummary(generateSummary);
        lecture.setStatus("uploading");
        lecture.setCreatedAt(LocalDateTime.now());
        lecture.setUpdatedAt(LocalDateTime.now());

        Lecture savedLecture = lectureRepository.save(lecture);
        logger.info("Lecture saved with ID: {}", savedLecture.getId());

        processLectureAsync(savedLecture, filePath.toString());

        return savedLecture;
    }

    @Async
    public void processLectureAsync(Lecture lecture, String filePath) {
        try {
            lecture.setStatus("processing");
            lecture.setUpdatedAt(LocalDateTime.now());
            lectureRepository.save(lecture);
            logger.info("Processing lecture: {} (file: {})", lecture.getId(), filePath);

            AIService.AIResponse aiResponse = aiService.processLecture(
                filePath,
                lecture.getLanguage(),
                lecture.isExtractTasks(),
                lecture.isGenerateSummary()
            );

            logger.info("AI response received. Duration: {} seconds", aiResponse.getDurationSeconds());

            // Store duration in lecture
            lecture.setDurationSeconds(aiResponse.getDurationSeconds());
            lectureRepository.save(lecture);

            // Create summary if present
            if (aiResponse != null && aiResponse.getSummary() != null) {
                Summary summary = new Summary();
                summary.setLectureId(lecture.getId());
                summary.setUserId(lecture.getUserId());
                summary.setContent(aiResponse.getSummary().getContent());
                summary.setKeyPoints(aiResponse.getSummary().getKeyPoints());
                summary.setTopics(aiResponse.getSummary().getTopics());
                summary.setTranscript(aiResponse.getTranscript());
                summary.setConfidence(aiResponse.getSummary().getConfidence());
                summary.setCreatedAt(LocalDateTime.now());

                Summary savedSummary = summaryRepository.save(summary);
                logger.info("Summary saved with ID: {} for lecture: {}", savedSummary.getId(), lecture.getId());

                // Update user stats using actual duration
                userRepository.findById(lecture.getUserId()).ifPresent(user -> {
                    User.UserStats stats = user.getStats();
                    stats.setTotalSummaries(stats.getTotalSummaries() + 1);
                    double hoursSaved = aiResponse.getDurationSeconds() / 3600.0;
                    stats.setHoursSaved(stats.getHoursSaved() + hoursSaved);
                    userRepository.save(user);
                    logger.info("User stats updated for user: {} (added {} hours, new total {})",
                            user.getId(), hoursSaved, stats.getHoursSaved());
                });
            } else {
                logger.warn("No summary in AI response for lecture: {}", lecture.getId());
            }

            // Create tasks if present
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
                    task.setCreatedAt(LocalDateTime.now());
                    task.setUpdatedAt(LocalDateTime.now());

                    Task savedTask = taskRepository.save(task);
                    logger.info("Task saved with ID: {} for lecture: {}", savedTask.getId(), lecture.getId());
                }
            } else {
                logger.info("No tasks to save for lecture: {}", lecture.getId());
            }

            lecture.setStatus("completed");
            lecture.setUpdatedAt(LocalDateTime.now());
            lectureRepository.save(lecture);
            logger.info("Lecture processing completed for ID: {}", lecture.getId());

        } catch (Exception e) {
            lecture.setStatus("failed");
            lecture.setUpdatedAt(LocalDateTime.now());
            lectureRepository.save(lecture);
            logger.error("Failed to process lecture: {}", lecture.getId(), e);
        }
    }

    public List<Lecture> getUserLectures(String email) {
        return userRepository.findByEmail(email)
                .map(user -> lectureRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Lecture getLecture(String id) {
        return lectureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lecture not found"));
    }

    public void deleteLecture(String id) {
        Lecture lecture = getLecture(id);

        Path filePath = Paths.get(uploadDir, lecture.getFileName());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filePath, e);
        }

        summaryRepository.deleteByLectureId(id);
        taskRepository.deleteByLectureId(id);
        lectureRepository.deleteById(id);
        logger.info("Lecture deleted: {}", id);
    }
}