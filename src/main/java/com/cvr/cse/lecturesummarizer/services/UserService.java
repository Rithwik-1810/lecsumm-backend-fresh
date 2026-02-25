package com.cvr.cse.lecturesummarizer.services;

import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    public User register(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists with this email");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        
        return userRepository.save(user);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateStats(String email, String type) {
        User user = getUserByEmail(email);
        
        switch (type) {
            case "summary":
                user.getStats().setTotalSummaries(user.getStats().getTotalSummaries() + 1);
                user.getStats().setHoursSaved(user.getStats().getHoursSaved() + 1);
                if (user.getStats().getTotalSummaries() == 10) {
                    user.getAchievements().add("10_SUMMARIES");
                }
                break;
            case "task_completed":
                user.getStats().setCompletedTasks(user.getStats().getCompletedTasks() + 1);
                if (user.getStats().getCompletedTasks() == 20) {
                    user.getAchievements().add("TASK_MASTER");
                }
                break;
        }
        
        return userRepository.save(user);
    }
}