// java
package com.example.backend.service;

import com.example.backend.dto.SpendingGoalProgressDTO;
import com.example.backend.model.Category;
import com.example.backend.model.SpendingGoal;
import com.example.backend.model.User;
import com.example.backend.model.GoalPeriod;
import com.example.backend.repository.CategoryRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.SpendingGoalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Extra unit tests to increase coverage of SpendingGoalService internal helpers
 * and evaluateHealth/evaluateAlert via getProgressForGoal().
 */
public class SpendingGoalServiceTest {

    @Mock
    private SpendingGoalRepository goalRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @Mock
    private ExpenseRecordRepository expenseRepo;

    @Mock
    private AchievementService achievementService;

    @InjectMocks
    private SpendingGoalService service;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setUser_id(1);

        category = new Category();
        category.setCategoryId(10);
        category.setCategoryName("Groceries");

        // basic save behavior to avoid NPEs if any test triggers save
        when(goalRepo.save(any(SpendingGoal.class))).thenAnswer(inv -> inv.getArgument(0));

        // ensure @Value minAmount is present in tests to avoid unrelated NPEs
        ReflectionTestUtils.setField(service, "minAmount", BigDecimal.valueOf(1.00));
    }

    // ---------- computeRange tests (private method) ----------
    @Test
    void computeRange_weekly_and_startNext_behaviour() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("computeRange", LocalDate.class, GoalPeriod.class, boolean.class);
        m.setAccessible(true);

        LocalDate today = LocalDate.of(2025, 10, 28); // Tuesday
        LocalDate[] range = (LocalDate[]) m.invoke(service, today, GoalPeriod.WEEKLY, false);

        // weekly start should be Monday of that week
        assertEquals(DayOfWeek.MONDAY, range[0].getDayOfWeek());
        assertEquals(range[0].plusDays(6), range[1]);

        // startNext true should advance to next week's Monday
        LocalDate[] nextRange = (LocalDate[]) m.invoke(service, today, GoalPeriod.WEEKLY, true);
        assertEquals(range[0].plusWeeks(1), nextRange[0]);
    }

    @Test
    void computeRange_monthly_and_yearly_behaviour() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("computeRange", LocalDate.class, GoalPeriod.class, boolean.class);
        m.setAccessible(true);

        LocalDate some = LocalDate.of(2025, 2, 10);

        LocalDate[] monthly = (LocalDate[]) m.invoke(service, some, GoalPeriod.MONTHLY, false);
        assertEquals(1, monthly[0].getDayOfMonth());
        assertEquals(monthly[0].plusMonths(1).minusDays(1), monthly[1]);

        LocalDate[] yearly = (LocalDate[]) m.invoke(service, some, GoalPeriod.YEARLY, false);
        assertEquals(LocalDate.of(2025, 1, 1), yearly[0]);
        assertEquals(LocalDate.of(2025, 12, 31), yearly[1]);
    }

    @Test
    void computeRange_nullPeriod_throwsInvocationTargetExceptionWithCause() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("computeRange", LocalDate.class, GoalPeriod.class, boolean.class);
        m.setAccessible(true);

        LocalDate today = LocalDate.now();
        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, today, null, false));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NullPointerException || ex.getCause() instanceof IllegalArgumentException);
    }

    // NEW: cover the startNext branches for MONTHLY and YEARLY
    @Test
    void computeRange_monthly_startNext_advancesToNextMonth() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("computeRange", LocalDate.class, GoalPeriod.class, boolean.class);
        m.setAccessible(true);

        LocalDate today = LocalDate.of(2025, 10, 10);
        LocalDate[] nextMonthly = (LocalDate[]) m.invoke(service, today, GoalPeriod.MONTHLY, true);

        // start should be first day of next month
        assertEquals(LocalDate.of(2025, 11, 1), nextMonthly[0]);
        // end should be last day of next month (November has 30)
        assertEquals(LocalDate.of(2025, 11, 30), nextMonthly[1]);
    }

    @Test
    void computeRange_yearly_startNext_advancesToNextYear() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("computeRange", LocalDate.class, GoalPeriod.class, boolean.class);
        m.setAccessible(true);

        LocalDate today = LocalDate.of(2025, 10, 10);
        LocalDate[] nextYearly = (LocalDate[]) m.invoke(service, today, GoalPeriod.YEARLY, true);

        // start should be Jan 1 of next year
        assertEquals(LocalDate.of(2026, 1, 1), nextYearly[0]);
        // end should be Dec 31 of next year
        assertEquals(LocalDate.of(2026, 12, 31), nextYearly[1]);
    }

    // ---------- buildGoalName tests (private method) ----------
    @Test
    void buildGoalName_nullOrBlankCategory_behaviour() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("buildGoalName", String.class, GoalPeriod.class);
        m.setAccessible(true);

        String name1 = (String) m.invoke(service, null, GoalPeriod.MONTHLY);
        assertTrue(name1.contains("Monthly"));

        String name2 = (String) m.invoke(service, "   ", GoalPeriod.WEEKLY);
        assertTrue(name2.contains("Weekly"));
    }

    @Test
    void buildGoalName_customCategoryAndPeriods() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("buildGoalName", String.class, GoalPeriod.class);
        m.setAccessible(true);

        String name = (String) m.invoke(service, "Dining", GoalPeriod.YEARLY);
        assertTrue(name.contains("Dining"));
        assertTrue(name.contains("Yearly"));
    }

    // ---------- isValidPeriodGoal tests (private method) ----------
    @Test
    void isValidPeriodGoal_nullGoal_throwsInvocationTargetException() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("isValidPeriodGoal", SpendingGoal.class);
        m.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, new Object[]{(SpendingGoal) null}));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void isValidPeriodGoal_variousInvalidAndValidCases() throws Exception {
        Method m = SpendingGoalService.class.getDeclaredMethod("isValidPeriodGoal", SpendingGoal.class);
        m.setAccessible(true);

        SpendingGoal g1 = new SpendingGoal();
        g1.setActive(false);
        Boolean r1 = (Boolean) m.invoke(service, g1);
        assertFalse(r1);

        SpendingGoal g2 = new SpendingGoal();
        g2.setActive(true);
        g2.setPeriod(null);
        Boolean r2 = (Boolean) m.invoke(service, g2);
        assertFalse(r2);

        SpendingGoal g3 = new SpendingGoal();
        g3.setActive(true);
        g3.setPeriod(GoalPeriod.MONTHLY);
        g3.setStartDate(null);
        g3.setEndDate(null);
        g3.setTargetAmount(BigDecimal.valueOf(10));
        g3.setCategory(category);
        Boolean r3 = (Boolean) m.invoke(service, g3);
        assertFalse(r3);

        SpendingGoal valid = new SpendingGoal();
        valid.setActive(true);
        valid.setPeriod(GoalPeriod.WEEKLY);
        valid.setStartDate(LocalDate.of(2025, 10, 1));
        valid.setEndDate(LocalDate.of(2025, 10, 7));
        valid.setTargetAmount(BigDecimal.valueOf(20));
        Category cat = new Category();
        cat.setCategoryId(99);
        valid.setCategory(cat);
        Boolean rValid = (Boolean) m.invoke(service, valid);
        assertTrue(rValid);
    }

    // ---------- listProgressForActiveGoals behaviour ----------
    @Test
    void listProgressForActiveGoals_filters_and_maps_only_valid() {
        SpendingGoal valid = new SpendingGoal();
        valid.setGoalId(101L);
        valid.setActive(true);
        valid.setPeriod(GoalPeriod.WEEKLY);
        valid.setStartDate(LocalDate.now().minusDays(6));
        valid.setEndDate(LocalDate.now());
        valid.setTargetAmount(BigDecimal.valueOf(100));
        valid.setCategory(category);
        valid.setUser(user);

        SpendingGoal invalid = new SpendingGoal();
        invalid.setGoalId(102L);
        invalid.setActive(true);
        invalid.setStartDate(LocalDate.now().minusDays(6));
        invalid.setEndDate(LocalDate.now());
        invalid.setTargetAmount(BigDecimal.valueOf(50));
        invalid.setCategory(category);
        invalid.setUser(user);

        when(goalRepo.findByUserAndActiveTrueOrderByCreatedAtDesc(user))
                .thenReturn(List.of(valid, invalid));

        SpendingGoalService spy = spy(service);
        SpendingGoalProgressDTO dtoForValid = new SpendingGoalProgressDTO(
                valid.getGoalId(),
                "Groceries",
                valid.getPeriod().name(),
                valid.getStartDate(),
                valid.getEndDate(),
                valid.getTargetAmount(),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(80),
                20.0,
                1L,
                "ON_TRACK",
                "NONE",
                80,
                100
        );

        doAnswer(inv -> {
            Long id = inv.getArgument(0);
            if (id.equals(valid.getGoalId())) return dtoForValid;
            throw new IllegalStateException("should not be called for invalid");
        }).when(spy).getProgressForGoal(anyLong(), eq(user));

        List<SpendingGoalProgressDTO> res = spy.listProgressForActiveGoals(user);
        assertEquals(1, res.size());
        assertEquals(valid.getGoalId(), res.get(0).goalId());
        assertEquals("Groceries", res.get(0).categoryName());

        verify(spy, times(1)).getProgressForGoal(valid.getGoalId(), user);
    }

    // ---------- getProgressForGoal -> exercise evaluateHealth/evaluateAlert ----------

    @Test
    void getProgressForGoal_overSpent_setsOverspentAndOverBudgetAlert() {
        Long gid = 201L;
        SpendingGoal goal = new SpendingGoal();
        goal.setGoalId(gid);
        goal.setUser(user);
        goal.setCategory(category);
        goal.setPeriod(GoalPeriod.WEEKLY);
        goal.setStartDate(LocalDate.now().minusDays(6));
        goal.setEndDate(LocalDate.now());
        goal.setTargetAmount(BigDecimal.valueOf(100));

        when(goalRepo.findById(gid)).thenReturn(Optional.of(goal));
        when(expenseRepo.sumByUserAndWindowAndCategoryId(user, goal.getStartDate(), goal.getEndDate(), category.getCategoryId()))
                .thenReturn(BigDecimal.valueOf(150));

        SpendingGoalProgressDTO dto = service.getProgressForGoal(gid, user);
        assertEquals("OVERSPENT", dto.health());
        assertEquals("OVER_BUDGET", dto.alertLevel());
        assertTrue(dto.progressPercent() > 100.0);
    }

    @Test
    void getProgressForGoal_atRisk_setsAtRiskAndWarningAlert() {
        Long gid = 202L;
        SpendingGoal goal = new SpendingGoal();
        goal.setGoalId(gid);
        goal.setUser(user);
        goal.setCategory(category);
        goal.setPeriod(GoalPeriod.WEEKLY);
        goal.setStartDate(LocalDate.now().minusDays(6));
        goal.setEndDate(LocalDate.now());
        goal.setTargetAmount(BigDecimal.valueOf(100));

        when(goalRepo.findById(gid)).thenReturn(Optional.of(goal));
        when(expenseRepo.sumByUserAndWindowAndCategoryId(user, goal.getStartDate(), goal.getEndDate(), category.getCategoryId()))
                .thenReturn(BigDecimal.valueOf(80)); // exactly WARNING_THRESHOLD

        SpendingGoalProgressDTO dto = service.getProgressForGoal(gid, user);
        assertEquals("AT_RISK", dto.health());
        assertEquals("WARNING", dto.alertLevel());
        assertEquals(80.0, dto.progressPercent(), 0.01);
    }

    @Test
    void getProgressForGoal_onTrack_setsOnTrackAndNoneAlert() {
        Long gid = 203L;
        SpendingGoal goal = new SpendingGoal();
        goal.setGoalId(gid);
        goal.setUser(user);
        goal.setCategory(category);
        goal.setPeriod(GoalPeriod.WEEKLY);
        goal.setStartDate(LocalDate.now().minusDays(6));
        goal.setEndDate(LocalDate.now());
        goal.setTargetAmount(BigDecimal.valueOf(100));

        when(goalRepo.findById(gid)).thenReturn(Optional.of(goal));
        when(expenseRepo.sumByUserAndWindowAndCategoryId(user, goal.getStartDate(), goal.getEndDate(), category.getCategoryId()))
                .thenReturn(BigDecimal.valueOf(20));

        SpendingGoalProgressDTO dto = service.getProgressForGoal(gid, user);
        assertEquals("ON_TRACK", dto.health());
        assertEquals("NONE", dto.alertLevel());
        assertTrue(dto.progressPercent() < 80.0);
    }
}
