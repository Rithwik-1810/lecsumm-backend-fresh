package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.Lecture;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.LectureRepository;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private AsyncProcessor asyncProcessor;

    public Lecture uploadLecture(String email, MultipartFile file, String title,
                                 String language, boolean extractTasks, boolean generateSummary) throws IOException {

        System.out.println(">>> UPLOAD ENDPOINT HIT for file: " + file.getOriginalFilename());
        logger.info(">>> UPLOAD ENDPOINT HIT for file: {}", file.getOriginalFilename());

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

        System.out.println(">>> ABOUT TO CALL PROCESSOR SYNC");
        asyncProcessor.processLecture(savedLecture, filePath.toString());
        System.out.println(">>> PROCESSOR CALL COMPLETED");

        return savedLecture;
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

        lectureRepository.deleteById(id);
        logger.info("Lecture deleted: {}", id);
    }
}