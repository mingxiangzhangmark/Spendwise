package com.example.backend.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.SecurityQuestionResponseDTO;
import com.example.backend.repository.QuestionRepository;

@RestController
@RequestMapping("/security-questions")
public class SecurityQuestionController {

    private final QuestionRepository questionRepository;

    public SecurityQuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @GetMapping
    public List<SecurityQuestionResponseDTO> getAllSecurityQuestions() {
        return questionRepository.findAll().stream()
                .map(question -> new SecurityQuestionResponseDTO(question.getId(), question.getQuestionText()))
                .collect(Collectors.toList());
    }
}
