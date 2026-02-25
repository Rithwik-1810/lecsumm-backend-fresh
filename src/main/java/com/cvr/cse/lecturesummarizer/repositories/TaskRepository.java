package com.cvr.cse.lecturesummarizer.repositories;

import com.cvr.cse.lecturesummarizer.models.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findByUserIdOrderByCreatedAtDesc(String userId);
    
    @Query("{'userId': ?0, 'status': ?1}")
    List<Task> findByUserIdAndStatus(String userId, String status);
    
    @Query("{'userId': ?0, 'priority': ?1}")
    List<Task> findByUserIdAndPriority(String userId, String priority);
    
    void deleteByLectureId(String lectureId);
    void deleteByUserId(String userId);
}