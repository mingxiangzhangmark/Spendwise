package com.example.backend.auth;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.backend.service.AchievementService;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AchievementService achievementService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AchievementService achievementService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.achievementService = achievementService;
    }

    /**
     * Attempt to authenticate a user.
     * @param identifier username or email
     * @param rawPassword plain text password
     * @return AuthResult (success with user OR failure with reason)
     */
    public AuthResult authenticate(String identifier, String rawPassword) {
        if (identifier == null || rawPassword == null) {
            return AuthResult.failure("Missing username or password");
        }

        User user = userRepository.findByUsernameOrEmail(identifier, identifier);
        if (user == null) {
            return AuthResult.failure("Invalid username/email");
        }

        boolean ok = passwordEncoder.matches(rawPassword, user.getPassword_hash());
        if (!ok) {
            return AuthResult.failure("Invalid password");
        }

        // Update last login
        user.setLast_login_at(LocalDateTime.now());
        userRepository.save(user);

        return AuthResult.success(user);
    }

    /**
     * Simple wrapper for authentication outcome.
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        private AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static AuthResult success(User user) {
            return new AuthResult(true, null, user);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }
}
