package com.example.backend.dto;

import com.example.backend.model.GoalPeriod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SpendingGoalResponse {
    private Long goalId;
    private Integer categoryId;
    private String categoryName;
    private GoalPeriod period;
    private BigDecimal targetAmount;
    private boolean active;
}
