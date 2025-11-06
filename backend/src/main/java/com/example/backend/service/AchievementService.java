// AchievementService.java
package com.example.backend.service;

import com.example.backend.model.Achievement;
import com.example.backend.model.User;
import com.example.backend.model.UserAchievement;
import com.example.backend.repository.AchievementRepository;
import com.example.backend.repository.UserAchievementRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.SpendingGoalRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ExpenseRecordRepository expenseRecordRepository;
    private final SpendingGoalRepository spendingGoalRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              ExpenseRecordRepository expenseRecordRepository,
                              SpendingGoalRepository spendingGoalRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.expenseRecordRepository = expenseRecordRepository;
        this.spendingGoalRepository = spendingGoalRepository;
    }

    public void earnIfNotEarned(Integer userId, String achievementCode) {
        boolean alreadyEarned = userAchievementRepository
                .findByUserIdAndAchievementCode(userId, achievementCode)
                .isPresent();

        if (!alreadyEarned) {
            Achievement achievement = achievementRepository.findByCode(achievementCode);
            if (achievement == null) throw new RuntimeException("Achievement not found");

            UserAchievement ua = new UserAchievement();
            ua.setUser(new User(userId)); // 只需要 id
            ua.setAchievement(achievement);
            ua.setEarned(true);
            ua.setEarnedAt(LocalDateTime.now());

            userAchievementRepository.save(ua);
        }
    }

    public void checkFirstExpense(Integer userId) {
        long count = expenseRecordRepository.countRecordsByUserId(userId);
        if (count == 1) {
            earnIfNotEarned(userId, "FIRST_EXPENSE");
        }
    }

    public void checkTenRecords(Integer userId) {
        long count = expenseRecordRepository.countRecordsByUserId(userId);
        if (count == 10) {
            earnIfNotEarned(userId, "TEN_RECORDS");
        }
    }

    public void checkSetGoal(Integer userId) {
        long count = spendingGoalRepository.countGoalsByUserId(userId);
        if (count == 1) {
            earnIfNotEarned(userId, "SET_GOAL");
        }
    }
}
