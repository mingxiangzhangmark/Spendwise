package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.model.SecurityQuestion;

public interface QuestionRepository extends JpaRepository<SecurityQuestion, Integer> {
    // SecurityQuestion findByQuestionText(String question_text);
}
