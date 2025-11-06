package com.example.backend.repository;

import com.example.backend.model.GoalPeriod;
import com.example.backend.model.SpendingGoal;
import com.example.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpendingGoalRepository extends JpaRepository<SpendingGoal, Long> {

    List<SpendingGoal> findByUserAndActiveTrueOrderByCreatedAtDesc(User user);

    Optional<SpendingGoal> findByUserAndCategory_CategoryIdAndPeriodAndActiveTrue(
            User user, Integer categoryId, GoalPeriod period
    );

    boolean existsByUserAndCategory_CategoryIdAndPeriodAndActiveTrue(
            User user, Integer categoryId, GoalPeriod period
    );

    @Query("SELECT COUNT(g) FROM SpendingGoal g WHERE g.user.user_id = :userId")
    long countGoalsByUserId(@Param("userId") Integer userId);
}
