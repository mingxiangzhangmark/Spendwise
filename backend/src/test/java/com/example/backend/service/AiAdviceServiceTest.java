package com.example.backend.service;

import com.example.backend.config.gemini.GeminiClient;
import com.example.backend.model.AiRecommendation;
import com.example.backend.model.FeatureSnapshot;
import com.example.backend.model.User;
import com.example.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AiAdviceServiceTest {

    @Mock private ExpenseRecordRepository expenseRepo;
    @Mock private AiRecommendationRepository recRepo;
    @Mock private FeatureSnapshotRepository snapshotRepo;
    @Mock private UserRepository userRepo;
    @Mock private GeminiClient gemini;

    @InjectMocks private AiAdviceService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private ExpenseRecordRepository.CategorySpend makeRow(int catId, String catName, double amt) {
        return new ExpenseRecordRepository.CategorySpend() {
            @Override public Integer getCategoryId() { return catId; }
            @Override public String getCategoryName() { return catName; }
            @Override public BigDecimal getAmount() { return BigDecimal.valueOf(amt); }
        };
    }

    @Test
    void testGenerate_noData() {
        int userId = 1;
        when(expenseRepo.findMonthlySpend(userId, 2025, 10)).thenReturn(List.of());
        var result = service.generate(userId, YearMonth.of(2025, 10), "en");
        assertTrue((Boolean) result.get("noData"));
        assertEquals("Insufficient transactions this month to generate suggestions.", result.get("message"));
        verify(expenseRepo, times(1)).findMonthlySpend(userId, 2025, 10);
    }

    @Test
    void testGenerate_normalFlow() throws Exception {
        int userId = 1;
        YearMonth ym = YearMonth.of(2025, 10);
        var rows = List.of(
                makeRow(1, "Food", 100),
                makeRow(2, "Travel", 300)
        );
        when(expenseRepo.findMonthlySpend(userId, ym.getYear(), ym.getMonthValue())).thenReturn(rows);

        User u = new User();
        u.setUser_id(userId);
        when(userRepo.findById(userId)).thenReturn(Optional.of(u));

        when(snapshotRepo.findByUserAndMonth(any(), any())).thenReturn(Optional.empty());
        when(recRepo.findByUserAndMonth(any(), any())).thenReturn(Optional.empty());

        when(gemini.generateAdviceJson(any(), any(), any(), any(), any()))
                .thenReturn("{\"summary\":\"ok\",\"bullets\":[{\"title\":\"t1\",\"detail\":\"d1\"}]}");

        var result = service.generate(userId, ym, "en");
        assertEquals("AUD", result.get("currency"));
        assertEquals(2, ((List<?>) result.get("totalsByCategory")).size());
        verify(snapshotRepo).save(any(FeatureSnapshot.class));
        verify(recRepo).save(any(AiRecommendation.class));
    }

    @Test
    void testGenerate_geminiThrowsException() throws Exception {
        int userId = 1;
        YearMonth ym = YearMonth.of(2025, 11);
        var rows = List.of(makeRow(1, "Food", 200));
        when(expenseRepo.findMonthlySpend(userId, ym.getYear(), ym.getMonthValue())).thenReturn(rows);

        User u = new User();
        u.setUser_id(userId);
        when(userRepo.findById(userId)).thenReturn(Optional.of(u));

        when(snapshotRepo.findByUserAndMonth(any(), any())).thenReturn(Optional.empty());
        when(recRepo.findByUserAndMonth(any(), any())).thenReturn(Optional.empty());

        when(gemini.generateAdviceJson(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("API_FAIL"));

        var result = service.generate(userId, ym, "en");
        assertTrue(result.get("summary").toString().contains("System is busy"));
        verify(snapshotRepo).save(any(FeatureSnapshot.class));
        verify(recRepo).save(any(AiRecommendation.class));
    }

        @Test
    void testReadJson_exceptionPath() throws Exception {
        AiAdviceService tmp = new AiAdviceService(
                expenseRepo, recRepo, snapshotRepo, userRepo, gemini);

        var m = AiAdviceService.class.getDeclaredMethod("readJson", String.class);
        m.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(tmp, "invalid_json"));

        Throwable cause = ex.getTargetException();
        assertTrue(cause instanceof RuntimeException);
        assertEquals("JSON_DECODE_FAILED", cause.getMessage());
    }

    @Test
    void testWriteJson_exceptionDirectly() throws Exception {
        AiAdviceService tmp = new AiAdviceService(
                expenseRepo, recRepo, snapshotRepo, userRepo, gemini);

        var m = AiAdviceService.class.getDeclaredMethod("writeJson", Object.class);
        m.setAccessible(true);

        Object bad = new Object() { public Object self = this; };

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> m.invoke(tmp, bad));

        Throwable cause = ex.getTargetException();
        assertTrue(cause instanceof RuntimeException);
        assertEquals("JSON_ENCODE_FAILED", cause.getMessage());
    }
}

