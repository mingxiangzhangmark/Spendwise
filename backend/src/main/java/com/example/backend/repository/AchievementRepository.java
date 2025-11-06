package com.example.backend.repository;

import com.example.backend.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Integer> {
    Achievement findByCode(String code);
    boolean existsByCode(String code);
}
