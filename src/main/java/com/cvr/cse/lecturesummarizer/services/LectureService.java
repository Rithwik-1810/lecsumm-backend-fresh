package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.Lecture;
import com.cvr.cse.lecturesummarizer.models.Summary;
import com.cvr.cse.lecturesummarizer.models.Task;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.LectureRepository;
import com.cvr.cse.lecturesummarizer.repositories.SummaryRepository;
import com.cvr.cse.lecturesummarizer.repositories.TaskRepository;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
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

        // Create upload directory if not exists
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.copy(file.getInputStream(), filePath);

        // Create lecture record
        Lecture lecture = new Lecture();
        lecture.setUserId(user.getId());
        lecture.setTitle(title != null ? title : file.getOriginalFilename());
        lecture.setFileName(fileName);
        lecture.setFileUrl("/uploads/" + fileName);
        lecture.setFileSize(file.getSize());
        
        // Fix line 72: Handle null content type
        String contentType = file.getContentType();
        lecture.setFileType(contentType != null && contentType.startsWith("video") ? "video" : "audio");
        
        lecture.setLanguage(language);
        lecture.setExtractTasks(extractTasks);
        lecture.setGenerateSummary(generateSummary);
        lecture.setStatus("uploading");
        lecture.setCreatedAt(LocalDateTime.now());
        lecture.setUpdatedAt(LocalDateTime.now());

        Lecture savedLecture = lectureRepository.save(lecture);

        // Process asynchronously
        processLectureAsync(savedLecture, filePath.toString());

        return savedLecture;
    }

    @Async
    public void processLectureAsync(Lecture lecture, String filePath) {
        try {
            // Update status to processing
            lecture.setStatus("processing");
            lecture.setUpdatedAt(LocalDateTime.now());
            lectureRepository.save(lecture);

            // Call Python AI service
            AIService.AIResponse aiResponse = aiService.processLecture(
                filePath, 
                lecture.getLanguage(),
                lecture.isExtractTasks(),
                lecture.isGenerateSummary()
            );

            // Create summary
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
                summaryRepository.save(summary);

                // Update user stats
                userRepository.findById(lecture.getUserId()).ifPresent(user -> {
                    user.getStats().setTotalSummaries(user.getStats().getTotalSummaries() + 1);
                    user.getStats().setHoursSaved(user.getStats().getHoursSaved() + 1);
                    userRepository.save(user);
                });
            }

            // Create tasks
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
                    taskRepository.save(task);
                }
            }

            // Update lecture status to completed
            lecture.setStatus("completed");
            lecture.setUpdatedAt(LocalDateTime.now());
            lectureRepository.save(lecture);

        } catch (Exception e) {
            lecture.setStatus("failed");
            lecture.setUpdatedAt(LocalDateTime.now());
            lectureRepository.save(lecture);
            e.printStackTrace();
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
        
        // Delete file
        Path filePath = Paths.get(uploadDir, lecture.getFileName());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Delete associated summaries and tasks
        summaryRepository.deleteByLectureId(id);
        taskRepository.deleteByLectureId(id);
        
        lectureRepository.deleteById(id);
    }
}