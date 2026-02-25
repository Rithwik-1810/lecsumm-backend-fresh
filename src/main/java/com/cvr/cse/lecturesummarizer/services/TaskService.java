package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.Task;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.TaskRepository;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Task> getUserTasks(String email, String status, String priority) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }
        
        String userId = userOpt.get().getId();
        
        if (status != null) {
            return taskRepository.findByUserIdAndStatus(userId, status);
        }
        if (priority != null) {
            return taskRepository.findByUserIdAndPriority(userId, priority);
        }
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Task getTask(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public Task createTask(String email, Task task) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found");
        }
        
        task.setUserId(userOpt.get().getId());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        return taskRepository.save(task);
    }

    public Task updateTask(String id, Task taskDetails) {
        Task task = getTask(id);
        
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setCourse(taskDetails.getCourse());
        task.setPriority(taskDetails.getPriority());
        task.setStatus(taskDetails.getStatus());
        task.setDeadline(taskDetails.getDeadline());
        task.setProgress(taskDetails.getProgress());
        task.setSubtasks(taskDetails.getSubtasks());
        task.setUpdatedAt(LocalDateTime.now());
        
        return taskRepository.save(task);
    }

    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }
}