package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseRecordDTO {
    private Long expenseId;
    private UserDTO user;
    private CategoryDTO category;
    private BigDecimal amount;
    private String currency;
    private LocalDate expenseDate;
    private String description;
    private Boolean isRecurring;
    private Integer recurringScheduleId;
    private String paymentMethod;
}
