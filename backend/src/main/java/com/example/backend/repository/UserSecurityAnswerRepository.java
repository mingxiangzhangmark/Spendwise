package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.model.User;
import com.example.backend.model.UserSecurityAnswer;

public interface UserSecurityAnswerRepository extends JpaRepository<UserSecurityAnswer, Integer> {
    UserSecurityAnswer findByUser(User user);
    // UserSecurityAnswer findByQuestionId(Integer questionId);
    UserSecurityAnswer findByUserAndQuestionId(User user, Integer questionId);
}
