package com.example.backend.service;

import com.example.backend.dto.CreateSpendingGoalRequest;
import com.example.backend.dto.SpendingGoalProgressDTO;
import com.example.backend.dto.SpendingGoalResponse;
import com.example.backend.model.*;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.SpendingGoalRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpendingGoalService {

    private final SpendingGoalRepository goalRepo;
    private final CategoryRepository categoryRepo;
    private final ExpenseRecordRepository expenseRepo;
    private final AchievementService achievementService;

    private static final int WARNING_THRESHOLD = 80;   // %
    private static final int OVER_BUDGET_THRESHOLD = 100; // %

    @Value("${goals.min-amount:1.00}")
    private BigDecimal minAmount;

    @Transactional(readOnly = true)
    public List<SpendingGoalResponse> listActiveGoals(User user) {
        return goalRepo.findByUserAndActiveTrueOrderByCreatedAtDesc(user)
                .stream().map(this::toResp).toList();
    }

    @Transactional
    public SpendingGoalResponse createGoal(User user, CreateSpendingGoalRequest req) {
        if (req.getTargetAmount() == null
                || req.getTargetAmount().compareTo(minAmount) < 0) {
            throw new ValidationException("Amount must be at least " + minAmount);
        }

        var category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));


        var existingOpt = goalRepo.findByUserAndCategory_CategoryIdAndPeriodAndActiveTrue(
                user, category.getCategoryId(), req.getPeriod()
        );

        if (existingOpt.isPresent() && !req.isConfirmDuplicate()) {

            throw new ValidationException(
                    "A goal for the same category and period already exists. " +
                            "Set confirmDuplicate=true to replace it."
            );
        }

        var start = req.getStartDate();
        var end = req.getEndDate();
        if (start == null || end == null) {
            var range = computeRange(LocalDate.now(), req.getPeriod(), req.isStartNextPeriod());
            start = range[0]; end = range[1];
        }

        // Deactivate existing goal before creating new one to avoid unique constraint violation
        existingOpt.ifPresent(old -> {
            old.setActive(false);
            goalRepo.saveAndFlush(old);  // Use saveAndFlush to ensure DB update happens immediately
        });

        var goal = new SpendingGoal();
        goal.setUser(user);
        goal.setCategory(category);
        goal.setPeriod(req.getPeriod());
        goal.setTargetAmount(req.getTargetAmount());
        goal.setGoalName(buildGoalName(category.getCategoryName(), req.getPeriod()));
        goal.setStartDate(start);
        goal.setEndDate(end);
        goal.setActive(true);

        var saved = goalRepo.save(goal);
        achievementService.checkSetGoal(user.getUser_id());
        return toResp(saved);
    }

    private LocalDate[] computeRange(LocalDate today, GoalPeriod period, boolean startNext) {
        if (startNext) {
            switch (period) {
                case WEEKLY -> today = today.with(java.time.DayOfWeek.MONDAY).plusWeeks(1);
                case MONTHLY -> today = today.withDayOfMonth(1).plusMonths(1);
                case YEARLY -> today = LocalDate.of(today.getYear() + 1, 1, 1);
            }
        }

        switch (period) {
            case WEEKLY -> {
                var start = today.with(java.time.DayOfWeek.MONDAY);
                var end = start.plusDays(6);
                return new LocalDate[]{start, end};
            }
            case MONTHLY -> {
                var start = today.withDayOfMonth(1);
                var end = start.plusMonths(1).minusDays(1);
                return new LocalDate[]{start, end};
            }
            case YEARLY -> {
                var start = LocalDate.of(today.getYear(), 1, 1);
                var end = LocalDate.of(today.getYear(), 12, 31);
                return new LocalDate[]{start, end};
            }
            default -> throw new IllegalArgumentException("Unsupported period");
        }
    }

    private SpendingGoalResponse toResp(SpendingGoal g) {
        var resp = new SpendingGoalResponse();
        resp.setGoalId(g.getGoalId());
        resp.setCategoryId(g.getCategory().getCategoryId());
        resp.setCategoryName(g.getCategory().getCategoryName());
        resp.setPeriod(g.getPeriod());
        resp.setTargetAmount(g.getTargetAmount());
        resp.setActive(g.isActive());
        return resp;
    }

      private String buildGoalName(String categoryName, GoalPeriod period) {
        var periodLabel = switch (period) {
            case WEEKLY -> "Weekly";
            case MONTHLY -> "Monthly";
            case YEARLY -> "Yearly";
        };
        return "%s %s Goal".formatted(categoryName, periodLabel);
    }


    // ------------------- progress tracking ------------------
    @Transactional(readOnly = true)
    public SpendingGoalProgressDTO getProgressForGoal(Long goalId, User user) {
        var goal = goalRepo.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
        assertOwnedBy(user, goal);
        validatePeriodGoal(goal);

        var start = goal.getStartDate();
        var end   = goal.getEndDate();
        var target = nz(goal.getTargetAmount());

        var catId = goal.getCategory().getCategoryId();
        var spent = expenseRepo.sumByUserAndWindowAndCategoryId(user, start, end, catId);

        var remaining = target.subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        double progressPct = toPercent(spent, target);
        long daysLeft = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), end) + 1);

        String health = evaluateHealth(spent, target);         // ON_TRACK / AT_RISK / OVERSPENT
        String alert  = evaluateAlert(progressPct);            // NONE / WARNING / OVER_BUDGET

        return new SpendingGoalProgressDTO(
                goal.getGoalId(),
                goal.getCategory().getCategoryName(),
                goal.getPeriod().name(),
                start,
                end,
                target,
                spent,
                remaining,
                round2(progressPct),
                daysLeft,
                health,
                alert,
                WARNING_THRESHOLD,
                OVER_BUDGET_THRESHOLD
        );
    }

    @Transactional(readOnly = true)
    public List<SpendingGoalProgressDTO> listProgressForActiveGoals(User user) {
        return goalRepo.findByUserAndActiveTrueOrderByCreatedAtDesc(user).stream()
                .filter(this::isValidPeriodGoal)
                .map(g -> getProgressForGoal(g.getGoalId(), user))
                .toList();
    }

    private void assertOwnedBy(User user, SpendingGoal goal) {
        if (!goal.getUser().getUser_id().equals(user.getUser_id())) {
            throw new ValidationException("Unauthorized: You can only access your own goals");
        }
    }
    private void validatePeriodGoal(SpendingGoal g) {
        if (g.getPeriod() == null || g.getStartDate() == null || g.getEndDate() == null) {
            throw new ValidationException("Only period-based goals are supported by Track API.");
        }
        if (g.getTargetAmount() == null || g.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Target amount must be positive.");
        }
    }
    private boolean isValidPeriodGoal(SpendingGoal g) {
        return g.isActive()
                && g.getPeriod() != null
                && g.getStartDate() != null
                && g.getEndDate() != null
                && g.getTargetAmount() != null
                && g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0;
    }
    private String evaluateHealth(BigDecimal spent, BigDecimal target) {
        if (spent.compareTo(target) > 0) return "OVERSPENT";
        double pct = toPercent(spent, target);
        if (pct >= WARNING_THRESHOLD) return "AT_RISK";
        return "ON_TRACK";
    }
    private String evaluateAlert(double progressPercent) {
        if (progressPercent > OVER_BUDGET_THRESHOLD) return "OVER_BUDGET";
        if (progressPercent >= WARNING_THRESHOLD)     return "WARNING";
        return "NONE";
    }
    private double toPercent(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
    private double round2(double v) {
        return new java.math.BigDecimal(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
