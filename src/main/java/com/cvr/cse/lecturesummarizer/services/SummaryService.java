package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.Summary;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.SummaryRepository;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SummaryService {

    @Autowired
    private SummaryRepository summaryRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Summary> getUserSummaries(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            return summaryRepository.findByUserIdOrderByCreatedAtDesc(userOpt.get().getId());
        }
        throw new RuntimeException("User not found");
    }

    public Summary getSummary(String id) {
        return summaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Summary not found"));
    }

    public Summary toggleSaveSummary(String id) {
        Summary summary = getSummary(id);
        summary.setSaved(!summary.isSaved());
        return summaryRepository.save(summary);
    }

    public void deleteSummary(String id) {
        summaryRepository.deleteById(id);
    }
}