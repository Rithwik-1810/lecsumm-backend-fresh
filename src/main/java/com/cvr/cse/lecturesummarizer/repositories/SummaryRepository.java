package com.cvr.cse.lecturesummarizer.repositories;

import com.cvr.cse.lecturesummarizer.models.Summary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryRepository extends MongoRepository<Summary, String> {
    List<Summary> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Summary> findByLectureId(String lectureId);
    void deleteByLectureId(String lectureId);
    void deleteByUserId(String userId);
}