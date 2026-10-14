package com.neighborconnect.neighborconnect_backend.controller;

import com.neighborconnect.neighborconnect_backend.dto.LoginRequest;
import com.neighborconnect.neighborconnect_backend.dto.RegisterRequest;
import com.neighborconnect.neighborconnect_backend.dto.UserResponse;
import com.neighborconnect.neighborconnect_backend.model.User;
import com.neighborconnect.neighborconnect_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.name() == null || req.name().isBlank()
                || req.email() == null || req.email().isBlank()
                || req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Name, email and a password of at least 6 characters are required"));
        }
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setPhone(req.phone());
        user.setNeighborhood(req.neighborhood());
        return ResponseEntity.ok(UserResponse.from(userRepository.save(user)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> found = req.email() == null ? Optional.empty() : userRepository.findByEmail(req.email());
        if (found.isEmpty() || req.password() == null || !found.get().isEnabled()
                || !passwordEncoder.matches(req.password(), found.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        return ResponseEntity.ok(UserResponse.from(found.get()));
    }
}