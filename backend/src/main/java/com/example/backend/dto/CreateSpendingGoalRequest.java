package com.example.backend.dto;

import com.example.backend.model.GoalPeriod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSpendingGoalRequest {

    @NotNull
    private Integer categoryId;

    @NotNull
    private GoalPeriod period; // WEEKLY/MONTHLY/YEARLY

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal targetAmount;

    private boolean confirmDuplicate = false;

    private LocalDate startDate;
    private LocalDate endDate;

    private boolean startNextPeriod;
}
