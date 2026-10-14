package com.neighborconnect.neighborconnect_backend.controller;

import com.neighborconnect.neighborconnect_backend.dto.AuthResponse;
import com.neighborconnect.neighborconnect_backend.dto.LoginRequest;
import com.neighborconnect.neighborconnect_backend.dto.RegisterRequest;
import com.neighborconnect.neighborconnect_backend.dto.UserResponse;
import com.neighborconnect.neighborconnect_backend.model.User;
import com.neighborconnect.neighborconnect_backend.repository.UserRepository;
import com.neighborconnect.neighborconnect_backend.service.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final TokenService tokenService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        String email = req.email() == null ? "" : req.email().trim();
        String password = req.password();

        if (name.isEmpty() || email.isEmpty() || !email.contains("@")
                || password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Name, a valid email and a password of at least 6 characters are required"));
        }
        if (password.length() > 72) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be 72 characters or fewer"));
        }
        if (name.length() > 255 || email.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name or email is too long"));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(blankToNull(req.phone()));
        user.setNeighborhood(blankToNull(req.neighborhood()));

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "Email already registered"));
        }
        return ResponseEntity.ok(new AuthResponse(tokenService.create(saved), UserResponse.from(saved)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String email = req.email() == null ? "" : req.email().trim();
        Optional<User> found = email.isEmpty() ? Optional.empty() : userRepository.findByEmail(email);
        if (found.isEmpty() || req.password() == null || req.password().length() > 72
                || !found.get().isEnabled()
                || !passwordEncoder.matches(req.password(), found.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        User user = found.get();
        return ResponseEntity.ok(new AuthResponse(tokenService.create(user), UserResponse.from(user)));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
