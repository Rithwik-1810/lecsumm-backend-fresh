package com.cvr.cse.lecturesummarizer.controllers;  // Changed from 'controller' to 'controllers'

import com.cvr.cse.lecturesummarizer.models.Lecture;
import com.cvr.cse.lecturesummarizer.services.LectureService;
import com.cvr.cse.lecturesummarizer.security.JwtUtil;  // Changed from 'utils' to 'security'
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
@CrossOrigin(origins = "http://localhost:5173")
public class LectureController {

    @Autowired
    private LectureService lectureService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadLecture(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "language", defaultValue = "english") String language,
            @RequestParam(value = "extractTasks", defaultValue = "true") boolean extractTasks,
            @RequestParam(value = "generateSummary", defaultValue = "true") boolean generateSummary) {
        
        try {
            String email = jwtUtil.extractUsername(token.substring(7));
            Lecture lecture = lectureService.uploadLecture(
                email, file, title, language, extractTasks, generateSummary);
            return ResponseEntity.ok(lecture);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserLectures(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractUsername(token.substring(7));
            List<Lecture> lectures = lectureService.getUserLectures(email);
            return ResponseEntity.ok(lectures);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getLecture(@PathVariable String id) {
        try {
            Lecture lecture = lectureService.getLecture(id);
            return ResponseEntity.ok(lecture);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLecture(@PathVariable String id) {
        try {
            lectureService.deleteLecture(id);
            return ResponseEntity.ok("Lecture deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}