package com.example.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SpendingGoalProgressDTO(
        Long goalId,
        String categoryName,
        String period,               // WEEKLY/MONTHLY/YEARLY
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal targetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,  // max(target - spent, 0)
        double progressPercent,      // 0~100+
        long daysLeft,               // inclusive today
        String health,               // ON_TRACK / AT_RISK / OVERSPENT
        String alertLevel,           // NONE / WARNING / OVER_BUDGET
        int warningThreshold,        // 80
        int overBudgetThreshold      // 100
) {}

