package com.example.backend.init;

import com.example.backend.model.Achievement;
import com.example.backend.repository.AchievementRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AchievementInitializer implements CommandLineRunner {

    private final AchievementRepository achievementRepository;

    public AchievementInitializer(AchievementRepository achievementRepository) {
        this.achievementRepository = achievementRepository;
    }

    @Override
    public void run(String... args) {
        List<Achievement> achievementsToInit = List.of(
                new Achievement("ACCOUNT_CREATED", "Welcome Aboard!", "You’ve successfully created your account.", "🏅"),
                new Achievement("FIRST_EXPENSE", "First Spend!", "You’ve logged your first expense.", "💸"),
                new Achievement("TEN_RECORDS", "Getting Serious", "You’ve added 10 records!", "📈"),
                new Achievement("SET_GOAL", "Goal Getter", "You’ve set your first spending goal.", "🎯")
        );

        for (Achievement a : achievementsToInit) {
            if (!achievementRepository.existsByCode(a.getCode())) {
                achievementRepository.save(a);
                System.out.println("✅ Insert achievement: " + a.getCode());
            }
        }
    }
}
