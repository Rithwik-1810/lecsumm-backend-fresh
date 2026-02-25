package com.cvr.cse.lecturesummarizer.controllers;

import com.cvr.cse.lecturesummarizer.models.Task;
import com.cvr.cse.lecturesummarizer.services.TaskService;
import com.cvr.cse.lecturesummarizer.security.JwtUtil;  // Changed from 'utils' to 'security'
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getUserTasks(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        try {
            String email = jwtUtil.extractUsername(token.substring(7));
            List<Task> tasks = taskService.getUserTasks(email, status, priority);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTask(@PathVariable String id) {
        try {
            Task task = taskService.getTask(id);
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestHeader("Authorization") String token,
            @RequestBody Task task) {
        try {
            String email = jwtUtil.extractUsername(token.substring(7));
            Task created = taskService.createTask(email, task);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable String id, @RequestBody Task task) {
        try {
            Task updated = taskService.updateTask(id, task);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        try {
            taskService.deleteTask(id);
            return ResponseEntity.ok("Task deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}