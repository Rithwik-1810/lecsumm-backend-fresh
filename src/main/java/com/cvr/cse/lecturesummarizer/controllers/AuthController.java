package com.cvr.cse.lecturesummarizer.controllers;

import com.cvr.cse.lecturesummarizer.dto.AuthRequest;
import com.cvr.cse.lecturesummarizer.dto.AuthResponse;
import com.cvr.cse.lecturesummarizer.models.User;
import com.cvr.cse.lecturesummarizer.security.JwtUtil;
import com.cvr.cse.lecturesummarizer.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            User user = userService.register(request.getName(), request.getEmail(), request.getPassword());
            String token = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(new AuthResponse(token, user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            UserDetails userDetails = userService.loadUserByUsername(request.getEmail());
            String token = jwtUtil.generateToken(userDetails.getUsername());
            User user = userService.getUserByEmail(request.getEmail());

            return ResponseEntity.ok(new AuthResponse(token, user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String token) {
        try {
            String email = jwtUtil.extractUsername(token.substring(7));
            User user = userService.getUserByEmail(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid token");
        }
    }
}