package com.example.backend.controller;

import com.example.backend.dto.ChangePasswordDTO;
import com.example.backend.dto.ResetPasswordConfirmDTO;
import com.example.backend.dto.ResetPasswordRequestDTO;
import com.example.backend.dto.SecurityQuestionResponseDTO;
import com.example.backend.model.User;
import com.example.backend.security.SessionUserResolver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.EntityNotFoundException;

import com.example.backend.service.PasswordResetService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/reset-password")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final SessionUserResolver sessionUserResolver;

    public PasswordResetController(PasswordResetService passwordResetService, SessionUserResolver sessionUserResolver) {
        this.passwordResetService = passwordResetService;
        this.sessionUserResolver = sessionUserResolver;
    }

    @PostMapping("/request")
    public ResponseEntity<SecurityQuestionResponseDTO> requestQuestion(@RequestBody ResetPasswordRequestDTO dto) {
        try {
            SecurityQuestionResponseDTO question = passwordResetService.getSecurityQuestion(dto.getIdentifier());
            return ResponseEntity.ok(question);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordConfirmDTO dto) {
        boolean success = passwordResetService.resetPassword(dto);
        if (success) {
            return ResponseEntity.ok("Password reset successful");
        } else {
            return ResponseEntity.status(401).body("Security answer incorrect");
        }
    }

    @PostMapping("/change")
    public ResponseEntity<String> changePassword(@RequestBody ChangePasswordDTO dto, HttpSession session) {
        try {
            User currentUser = sessionUserResolver.getCurrentUser(session);
            boolean success = passwordResetService.changePassword(
                currentUser.getUser_id(), 
                dto.getCurrentPassword(), 
                dto.getNewPassword()
            );
            
            if (success) {
                return ResponseEntity.ok("Password updated successfully");
            } else {
                return ResponseEntity.status(401).body("Current password is incorrect");
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body("User not logged in");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred: " + e.getMessage());
        }
    }
}
