package com.example.backend.service;

import com.example.backend.dto.ResetPasswordConfirmDTO;
import com.example.backend.dto.SecurityQuestionResponseDTO;
import com.example.backend.model.User;
import com.example.backend.model.SecurityQuestion;
import com.example.backend.model.UserSecurityAnswer;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserSecurityAnswerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final UserSecurityAnswerRepository answerRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                UserSecurityAnswerRepository answerRepository,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.answerRepository = answerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SecurityQuestionResponseDTO getSecurityQuestion(String identifier) {
        User user = userRepository.findByUsernameOrEmail(identifier, identifier);
        if (user == null) {
            throw new EntityNotFoundException("User not found");
        }

        UserSecurityAnswer answer = answerRepository.findByUser(user);
        if (answer == null || answer.getQuestion() == null) {
            throw new EntityNotFoundException("Security question not configured");
        }

        SecurityQuestion question = answer.getQuestion();
        return new SecurityQuestionResponseDTO(question.getId(), question.getQuestionText());
    }

    public boolean resetPassword(ResetPasswordConfirmDTO dto) {
        User user = userRepository.findByUsernameOrEmail(dto.getIdentifier(), dto.getIdentifier());
        if (user == null) return false;

        UserSecurityAnswer storedAnswer =
            answerRepository.findByUserAndQuestionId(user, dto.getQuestionId());

        if (storedAnswer == null) return false;

        boolean match = passwordEncoder.matches(dto.getAnswer(), storedAnswer.getAnswer_hash());
        if (!match) return false;

        user.setPassword_hash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        return true;
    }

    public boolean changePassword(Integer userId, String currentPassword, String newPassword) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found"));
            
          
            if (!passwordEncoder.matches(currentPassword, user.getPassword_hash())) {
                return false; 
            }
            
            
            user.setPassword_hash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
