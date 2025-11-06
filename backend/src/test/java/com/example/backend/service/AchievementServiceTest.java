package com.example.backend.service;

import com.example.backend.model.Achievement;
import com.example.backend.model.UserAchievement;
import com.example.backend.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private ExpenseRecordRepository expenseRecordRepository;
    @Mock private SpendingGoalRepository spendingGoalRepository;

    @InjectMocks private AchievementService achievementService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testEarnIfNotEarned_shouldSaveWhenNotEarned() {
        Integer userId = 1;
        when(userAchievementRepository.findByUserIdAndAchievementCode(userId, "TEST"))
                .thenReturn(Optional.empty());
        when(achievementRepository.findByCode("TEST"))
                .thenReturn(new Achievement("TEST", "t", "d", null));

        achievementService.earnIfNotEarned(userId, "TEST");
        verify(userAchievementRepository, times(1)).save(any(UserAchievement.class));
    }

    @Test
    void testEarnIfNotEarned_alreadyEarnedShouldNotSave() {
        Integer userId = 1;
        when(userAchievementRepository.findByUserIdAndAchievementCode(userId, "ALREADY"))
                .thenReturn(Optional.of(new UserAchievement()));

        achievementService.earnIfNotEarned(userId, "ALREADY");
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void testEarnIfNotEarned_achievementNotFound() {
        Integer userId = 1;
        when(userAchievementRepository.findByUserIdAndAchievementCode(userId, "NONE"))
                .thenReturn(Optional.empty());
        when(achievementRepository.findByCode("NONE")).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> achievementService.earnIfNotEarned(userId, "NONE"));
    }

    @Test
    void testCheckFirstExpense_shouldTriggerEarn() {
        when(expenseRecordRepository.countRecordsByUserId(1)).thenReturn(1L);
        when(userAchievementRepository.findByUserIdAndAchievementCode(1, "FIRST_EXPENSE"))
                .thenReturn(Optional.empty());
        when(achievementRepository.findByCode("FIRST_EXPENSE"))
                .thenReturn(new Achievement("FIRST_EXPENSE", "First", "desc", null));

        achievementService.checkFirstExpense(1);
        verify(userAchievementRepository).save(any());
    }

    @Test
    void testCheckFirstExpense_noTrigger() {
        when(expenseRecordRepository.countRecordsByUserId(1)).thenReturn(2L);
        achievementService.checkFirstExpense(1);
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void testCheckTenRecords_shouldTriggerEarn() {
        when(expenseRecordRepository.countRecordsByUserId(1)).thenReturn(10L);
        when(userAchievementRepository.findByUserIdAndAchievementCode(1, "TEN_RECORDS"))
                .thenReturn(Optional.empty());
        when(achievementRepository.findByCode("TEN_RECORDS"))
                .thenReturn(new Achievement("TEN_RECORDS", "Ten", "desc", null));

        achievementService.checkTenRecords(1);
        verify(userAchievementRepository).save(any());
    }

    @Test
    void testCheckTenRecords_noTrigger() {
        when(expenseRecordRepository.countRecordsByUserId(1)).thenReturn(5L);
        achievementService.checkTenRecords(1);
        verify(userAchievementRepository, never()).save(any());
    }

    @Test
    void testCheckSetGoal_shouldEarnAchievementWhenFirstGoalSet() {
        Integer userId = 1;
        when(spendingGoalRepository.countGoalsByUserId(userId)).thenReturn(1L);
        when(userAchievementRepository.findByUserIdAndAchievementCode(userId, "SET_GOAL"))
                .thenReturn(Optional.empty());
        when(achievementRepository.findByCode("SET_GOAL"))
                .thenReturn(new Achievement("SET_GOAL", "Set Goal", "Set a spending goal", null));

        achievementService.checkSetGoal(userId);
        verify(userAchievementRepository, times(1)).save(any(UserAchievement.class));
    }

    @Test
    void testCheckSetGoal_noTrigger() {
        Integer userId = 1;
        when(spendingGoalRepository.countGoalsByUserId(userId)).thenReturn(0L);
        achievementService.checkSetGoal(userId);
        verify(userAchievementRepository, never()).save(any());
    }
}
