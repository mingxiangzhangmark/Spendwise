package com.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Integer expenseId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(length = 255)
    private String description;

    @Column(length = 500)
    private String notes;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "is_recurring")
    private Boolean isRecurring;

    @ManyToOne
    @JoinColumn(name = "recurring_schedule_id")
    private RecurringExpenseSchedule recurringSchedule;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
