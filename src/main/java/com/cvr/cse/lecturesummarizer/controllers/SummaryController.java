package com.cvr.cse.lecturesummarizer.controllers;

import com.cvr.cse.lecturesummarizer.models.Summary;
import com.cvr.cse.lecturesummarizer.services.SummaryService;
import com.cvr.cse.lecturesummarizer.security.JwtUtil;
import com.cvr.cse.lecturesummarizer.repositories.SummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/summaries")
@CrossOrigin(origins = "http://localhost:5173")
public class SummaryController {

    @Autowired
    private SummaryService summaryService;

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getUserSummaries(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractUsername(token.substring(7));
            List<Summary> summaries = summaryService.getUserSummaries(email);
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSummary(@PathVariable String id) {
        try {
            Summary summary = summaryService.getSummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<?> getSummaryByLectureId(@PathVariable String lectureId) {
        try {
            List<Summary> summaries = summaryRepository.findByLectureId(lectureId);
            if (summaries.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(summaries.get(0));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/save")
    public ResponseEntity<?> toggleSaveSummary(@PathVariable String id) {
        try {
            Summary summary = summaryService.toggleSaveSummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSummary(@PathVariable String id) {
        try {
            summaryService.deleteSummary(id);
            return ResponseEntity.ok("Summary deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}