package com.example.backend.service;

import com.example.backend.model.Category;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.RecurringExpenseSchedule;
import com.example.backend.model.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.RecurringExpenseScheduleRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private static final ZoneId ZONE = ZoneId.of("Australia/Sydney");

    private final RecurringExpenseScheduleRepository scheduleRepo;
    private final ExpenseRecordRepository expenseRepo;
    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;

    /** 供 Controller/ExpenseRecordService 在“手工创建账单成功后”调用 */
    public void onManualExpenseSaved(ExpenseRecord savedRecord,
                                     RecurringExpenseSchedule.Frequency freq) {
        Integer userId = savedRecord.getUser().getUser_id();
        Integer categoryId = savedRecord.getCategory().getCategoryId();
        LocalDate expenseDate = savedRecord.getExpenseDate();

        // 1) 候选计划：同 user + category + frequency
        List<RecurringExpenseSchedule> candidates =
                scheduleRepo.findByUserCategoryFrequency(userId, categoryId, freq);

        // 2) 严格匹配：只有 nextRunDate == expenseDate 的计划才推进
        RecurringExpenseSchedule exact =
                candidates.stream()
                        .filter(s -> expenseDate.equals(s.getNextRunDate()))
                        .min(Comparator.comparing(RecurringExpenseSchedule::getNextRunDate))
                        .orElse(null);

        if (exact != null) {
            bindAndAdvance(savedRecord, exact);
            return;
        }

        // 3) 未命中则新建计划（以本次 expenseDate 作为锚点，nextRunDate=下一期）
        RecurringExpenseSchedule s = new RecurringExpenseSchedule();
        s.setUser(savedRecord.getUser());
        s.setCategory(savedRecord.getCategory());
        s.setAmount(savedRecord.getAmount());
        s.setCurrency(savedRecord.getCurrency());
        s.setPaymentMethod(savedRecord.getPaymentMethod());
        s.setNotes(savedRecord.getNotes());
        s.setFrequency(freq);
        s.setStartDate(expenseDate);
        s.setEndDate(null);
        s.setNextRunDate(nextRunAfter(s, expenseDate));
        scheduleRepo.save(s);

        savedRecord.setIsRecurring(Boolean.TRUE);
        savedRecord.setRecurringSchedule(s);
        expenseRepo.save(savedRecord);
    }

    private void bindAndAdvance(ExpenseRecord rec, RecurringExpenseSchedule s) {
        rec.setIsRecurring(Boolean.TRUE);
        rec.setRecurringSchedule(s);
        expenseRepo.save(rec);

        s.setLastRunAt(LocalDateTime.now(ZONE));
        s.setNextRunDate(nextRunAfter(s, s.getNextRunDate()));
        scheduleRepo.save(s);
    }

    /** 定时任务：每天 00:05 执行到期计划 */
    @Scheduled(cron = "0 5 0 * * *", zone = "Australia/Sydney")
    public void processDueSchedules() {
        LocalDate today = LocalDate.now(ZONE);
        List<RecurringExpenseSchedule> due = scheduleRepo.findDueNoStatus(today);

        for (RecurringExpenseSchedule s : due) {
            ExpenseRecord rec = new ExpenseRecord();
            rec.setUser(s.getUser());
            rec.setCategory(s.getCategory());
            rec.setAmount(s.getAmount());
            rec.setCurrency(s.getCurrency());
            rec.setExpenseDate(s.getNextRunDate());
            rec.setDescription(buildAutoDescription(s));
            rec.setNotes(s.getNotes());
            rec.setPaymentMethod(s.getPaymentMethod());
            rec.setIsRecurring(Boolean.TRUE);
            rec.setRecurringSchedule(s);
            expenseRepo.save(rec);

            s.setLastRunAt(LocalDateTime.now(ZONE));
            s.setNextRunDate(nextRunAfter(s, s.getNextRunDate()));
            scheduleRepo.save(s);
        }
    }

    /** 可选：直接后端创建计划（有独立入口时用） */
    public RecurringExpenseSchedule createSchedule(Integer userId,
                                                   Integer categoryId,
                                                   BigDecimal amount,
                                                   String currency,
                                                   RecurringExpenseSchedule.Frequency frequency,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   String paymentMethod,
                                                   String notes) {
        User user = userRepo.findById(userId).orElseThrow();
        Category category = categoryRepo.findById(categoryId).orElseThrow();

        RecurringExpenseSchedule s = new RecurringExpenseSchedule();
        s.setUser(user);
        s.setCategory(category);
        s.setAmount(amount);
        s.setCurrency(currency);
        s.setFrequency(frequency);
        s.setStartDate(startDate);
        s.setEndDate(endDate);
        s.setPaymentMethod(paymentMethod);
        s.setNotes(notes);

        LocalDate today = LocalDate.now(ZONE);
        s.setNextRunDate(computeFirstNextRunDate(today, s));
        return scheduleRepo.save(s);
    }

    /** 取消计划：解绑历史账单（保留），再删除该计划 */
    @Transactional
    public void cancelSchedule(Integer scheduleId) {
        // 先把该计划下的账单取消绑定（并设 isRecurring=false）
        expenseRepo.detachSchedule(scheduleId);
        // 删除计划
        scheduleRepo.deleteById(scheduleId);
    }

    // —— 推进规则 —— //
    private LocalDate nextRunAfter(RecurringExpenseSchedule s, LocalDate last) {
        return switch (s.getFrequency()) {
            case DAILY   -> last.plusDays(1);
            case WEEKLY  -> last.plusWeeks(1);
            case MONTHLY -> {
                int anchorDom = s.getStartDate().getDayOfMonth();
                LocalDate next = last.plusMonths(1);
                yield next.withDayOfMonth(Math.min(anchorDom, next.lengthOfMonth()));
            }
        };
    }

    private LocalDate computeFirstNextRunDate(LocalDate today, RecurringExpenseSchedule s) {
        LocalDate d = s.getStartDate();
        if (d.isBefore(today)) {
            while (d.isBefore(today)) {
                d = nextRunAfter(s, d);
            }
        }
        return d;
    }

    private String buildAutoDescription(RecurringExpenseSchedule s) {
        String cat = (s.getCategory() != null && s.getCategory().getCategoryName() != null)
                ? s.getCategory().getCategoryName() : "Recurring";
        return "[Auto] " + cat + " / " + s.getFrequency().name();
    }
}
