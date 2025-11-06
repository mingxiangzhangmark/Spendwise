package com.example.backend.auth;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        AchievementService achievementService = mock(AchievementService.class);
        authService = new AuthService(userRepository, passwordEncoder, achievementService);
    }

    @Test
    void authenticate_success_updatesLastLogin_andSaves() {
        String identifier = "user@example.com";
        String rawPwd = "plainPwd";

        User user = new User();
        user.setPassword_hash("$2a$dummy.hash");

        when(userRepository.findByUsernameOrEmail(identifier, identifier)).thenReturn(user);
        when(passwordEncoder.matches(rawPwd, user.getPassword_hash())).thenReturn(true);

        AuthService.AuthResult result = authService.authenticate(identifier, rawPwd);

        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void authenticate_fail_whenUserNotFound() {
        String identifier = "ghost";
        when(userRepository.findByUsernameOrEmail(identifier, identifier)).thenReturn(null);

        AuthService.AuthResult result = authService.authenticate(identifier, "anything");

        assertFalse(result.isSuccess());
        assertEquals("Invalid username/email", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_fail_whenPasswordMismatch() {
        String identifier = "user";
        User user = new User();
        user.setPassword_hash("$2a$hash");
        when(userRepository.findByUsernameOrEmail(identifier, identifier)).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword_hash())).thenReturn(false);

        AuthService.AuthResult result = authService.authenticate(identifier, "wrong");

        assertFalse(result.isSuccess());
        assertEquals("Invalid password", result.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_fail_whenMissingParams() {
        AuthService.AuthResult r1 = authService.authenticate(null, "pwd");
        AuthService.AuthResult r2 = authService.authenticate("user", null);

        assertFalse(r1.isSuccess());
        assertEquals("Missing username or password", r1.getMessage());

        assertFalse(r2.isSuccess());
        assertEquals("Missing username or password", r2.getMessage());

        verify(userRepository, never()).save(any());
    }
}
