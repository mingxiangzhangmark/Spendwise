// java
package com.example.backend.service;

import com.example.backend.model.Category;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.RecurringExpenseSchedule;
import com.example.backend.model.User;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.RecurringExpenseScheduleRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RecurringExpenseServiceTest {

    @Mock
    private RecurringExpenseScheduleRepository scheduleRepo;

    @Mock
    private ExpenseRecordRepository expenseRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @InjectMocks
    private RecurringExpenseService service;

    private static final ZoneId ZONE = ZoneId.of("Australia/Sydney");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // default behaviour: save returns the same entity passed in
        when(scheduleRepo.save(any(RecurringExpenseSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(expenseRepo.save(any(ExpenseRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testOnManualExpenseSaved_createsNewScheduleWhenNoExactMatch() {
        // given
        LocalDate expenseDate = LocalDate.now(ZONE).minusDays(1);
        User user = new User();
        user.setUser_id(100);

        Category cat = new Category();
        cat.setCategoryId(20);

        ExpenseRecord savedRecord = new ExpenseRecord();
        savedRecord.setUser(user);
        savedRecord.setCategory(cat);
        savedRecord.setAmount(BigDecimal.valueOf(12.5));
        savedRecord.setCurrency("AUD");
        savedRecord.setPaymentMethod("card");
        savedRecord.setNotes("note");
        savedRecord.setExpenseDate(expenseDate);

        when(scheduleRepo.findByUserCategoryFrequency(user.getUser_id(), cat.getCategoryId(),
                RecurringExpenseSchedule.Frequency.DAILY))
                .thenReturn(Collections.emptyList());

        // when
        service.onManualExpenseSaved(savedRecord, RecurringExpenseSchedule.Frequency.DAILY);

        // then: schedule saved and savedRecord linked
        ArgumentCaptor<RecurringExpenseSchedule> cap = ArgumentCaptor.forClass(RecurringExpenseSchedule.class);
        verify(scheduleRepo, times(1)).save(cap.capture());
        RecurringExpenseSchedule savedSchedule = cap.getValue();

        assertEquals(user, savedSchedule.getUser());
        assertEquals(cat, savedSchedule.getCategory());
        assertEquals(RecurringExpenseSchedule.Frequency.DAILY, savedSchedule.getFrequency());
        // nextRunDate should be next after expenseDate for DAILY
        assertEquals(expenseDate.plusDays(1), savedSchedule.getNextRunDate());

        assertTrue(Boolean.TRUE.equals(savedRecord.getIsRecurring()));
        assertEquals(savedSchedule, savedRecord.getRecurringSchedule());

        verify(expenseRepo, times(1)).save(savedRecord);
    }

    @Test
    void testOnManualExpenseSaved_bindsExactAndAdvancesSchedule() {
        // given
        LocalDate expenseDate = LocalDate.now(ZONE);
        User user = new User();
        user.setUser_id(2);

        Category cat = new Category();
        cat.setCategoryId(3);

        ExpenseRecord savedRecord = new ExpenseRecord();
        savedRecord.setUser(user);
        savedRecord.setCategory(cat);
        savedRecord.setExpenseDate(expenseDate);

        RecurringExpenseSchedule schedule = new RecurringExpenseSchedule();
        schedule.setUser(user);
        schedule.setCategory(cat);
        schedule.setFrequency(RecurringExpenseSchedule.Frequency.DAILY);
        schedule.setStartDate(expenseDate);
        schedule.setNextRunDate(expenseDate);

        when(scheduleRepo.findByUserCategoryFrequency(user.getUser_id(), cat.getCategoryId(),
                RecurringExpenseSchedule.Frequency.DAILY))
                .thenReturn(List.of(schedule));

        // when
        service.onManualExpenseSaved(savedRecord, RecurringExpenseSchedule.Frequency.DAILY);

        // then
        verify(expenseRepo, times(1)).save(savedRecord);
        verify(scheduleRepo, times(1)).save(schedule);
        assertTrue(Boolean.TRUE.equals(savedRecord.getIsRecurring()));
        assertEquals(schedule, savedRecord.getRecurringSchedule());

        // schedule should have advanced to next day
        assertEquals(expenseDate.plusDays(1), schedule.getNextRunDate());
        assertNotNull(schedule.getLastRunAt());
        assertTrue(schedule.getLastRunAt() instanceof LocalDateTime);
    }

    @Test
    void testProcessDueSchedules_createsExpenseAndAdvancesSchedule() {
        // given
        LocalDate today = LocalDate.now(ZONE);
        User user = new User();
        user.setUser_id(5);

        Category cat = new Category();
        cat.setCategoryId(7);
        cat.setCategoryName("Food");

        RecurringExpenseSchedule s = new RecurringExpenseSchedule();
        s.setUser(user);
        s.setCategory(cat);
        s.setAmount(BigDecimal.valueOf(30));
        s.setCurrency("AUD");
        s.setPaymentMethod("cash");
        s.setNotes("monthly");
        s.setFrequency(RecurringExpenseSchedule.Frequency.DAILY);
        s.setNextRunDate(today);

        when(scheduleRepo.findDueNoStatus(today)).thenReturn(List.of(s));

        // when
        service.processDueSchedules();

        // then: an expense record saved and schedule advanced
        ArgumentCaptor<ExpenseRecord> recCap = ArgumentCaptor.forClass(ExpenseRecord.class);
        verify(expenseRepo, times(1)).save(recCap.capture());
        ExpenseRecord created = recCap.getValue();

        assertEquals(user, created.getUser());
        assertEquals(cat, created.getCategory());
        assertEquals(s.getAmount(), created.getAmount());
        assertEquals(s.getCurrency(), created.getCurrency());
        assertEquals(today, created.getExpenseDate());
        assertEquals("[Auto] Food / DAILY", created.getDescription());
        assertTrue(Boolean.TRUE.equals(created.getIsRecurring()));
        assertEquals(s, created.getRecurringSchedule());

        verify(scheduleRepo, times(1)).save(s);
        assertEquals(today.plusDays(1), s.getNextRunDate());
        assertNotNull(s.getLastRunAt());
    }

    @Test
    void testCreateSchedule_setsNextRunDate_whenStartIsToday() {
        // given
        LocalDate today = LocalDate.now(ZONE);
        User user = new User();
        user.setUser_id(9);
        Category cat = new Category();
        cat.setCategoryId(11);

        when(userRepo.findById(user.getUser_id())).thenReturn(Optional.of(user));
        when(categoryRepo.findById(cat.getCategoryId())).thenReturn(Optional.of(cat));

        // ensure scheduleRepo.save returns its argument (configured in setUp)

        // when
        RecurringExpenseSchedule result = service.createSchedule(
                user.getUser_id(),
                cat.getCategoryId(),
                BigDecimal.valueOf(5),
                "AUD",
                RecurringExpenseSchedule.Frequency.MONTHLY,
                today,
                null,
                "card",
                "notes"
        );

        // then: nextRunDate should equal startDate because startDate == today
        assertEquals(today, result.getNextRunDate());
        assertEquals(user, result.getUser());
        assertEquals(cat, result.getCategory());
    }

    @Test
    void testCancelSchedule_detachesAndDeletes() {
        Integer scheduleId = 123;
        // when
        service.cancelSchedule(scheduleId);

        // then
        verify(expenseRepo, times(1)).detachSchedule(scheduleId);
        verify(scheduleRepo, times(1)).deleteById(scheduleId);
    }
}