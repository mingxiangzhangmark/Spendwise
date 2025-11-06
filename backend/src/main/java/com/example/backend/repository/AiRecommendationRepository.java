package com.example.backend.repository;

import com.example.backend.model.AiRecommendation;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {
    Optional<AiRecommendation> findByUserAndMonth(User user, String month);
}
