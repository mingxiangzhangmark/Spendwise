package com.example.backend.service;

import com.example.backend.dto.RegisterDTO;
import com.example.backend.model.User;
import com.example.backend.model.SecurityQuestion;
import com.example.backend.model.UserSecurityAnswer;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.QuestionRepository;
import com.example.backend.repository.UserSecurityAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RegisterService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuestionRepository questionRepository;
    private final UserSecurityAnswerRepository userSecurityAnswerRepository;
    private final AchievementService achievementService; //

    @Autowired
    public RegisterService(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           QuestionRepository questionRepository,
                           UserSecurityAnswerRepository userSecurityAnswerRepository,
                           AchievementService achievementService) { //
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.questionRepository = questionRepository;
        this.userSecurityAnswerRepository = userSecurityAnswerRepository;
        this.achievementService = achievementService;
    }

    public User register(RegisterDTO dto) {

        if (userRepository.findByUsername(dto.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(dto.getEmail()) != null) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone_number(dto.getPhoneNumber());
        user.setPassword_hash(passwordEncoder.encode(dto.getPassword()));

        LocalDateTime now = LocalDateTime.now();
        user.setCreated_at(now);
        user.setUpdated_at(now);

        User savedUser = userRepository.save(user);

        if (dto.getQuestionId() != null && dto.getAnswer() != null) {
            SecurityQuestion question = questionRepository.findById(dto.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Invalid security question"));

            UserSecurityAnswer answer = new UserSecurityAnswer();
            answer.setUser(savedUser);
            answer.setQuestion(question);
            answer.setAnswer_hash(passwordEncoder.encode(dto.getAnswer()));
            userSecurityAnswerRepository.save(answer);
        }

        try {
            achievementService.earnIfNotEarned(savedUser.getUser_id(), "ACCOUNT_CREATED");
        } catch (Exception e) {
            System.err.println("Failed to grant ACCOUNT_CREATED achievement: " + e.getMessage());
        }

        return savedUser;
    }
}
