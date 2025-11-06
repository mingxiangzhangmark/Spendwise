package com.example.backend.service;

import com.example.backend.dto.ExpenseReportDTO;
import com.example.backend.model.*;
import com.example.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExpenseRecordServiceTest {

    @Mock private ExpenseRecordRepository expenseRepo;
    @Mock private UserRepository userRepo;
    @Mock private CategoryRepository categoryRepo;
    @Mock private AchievementService achievementService;

    @InjectMocks private ExpenseRecordService service;

    private User mockUser;
    private Category mockCat;
    private ExpenseRecord mockRecord;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUser = new User();
        mockUser.setUser_id(1);

        mockCat = new Category();
        mockCat.setCategoryId(100);
        mockCat.setCategoryName("Food");

        mockRecord = new ExpenseRecord();
        mockRecord.setExpenseId(10);
        mockRecord.setUser(mockUser);
        mockRecord.setCategory(mockCat);
        mockRecord.setAmount(BigDecimal.valueOf(100));
    }

    @Test
    void testGetRecordsForUser_success() {
        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findByUser(mockUser)).thenReturn(List.of(mockRecord));

        var result = service.getRecordsForUser(1);
        assertEquals(1, result.size());
        verify(expenseRepo).findByUser(mockUser);
    }

    @Test
    void testGetRecordsForUser_userNotFound() {
        when(userRepo.findById(1)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.getRecordsForUser(1));
    }

    @Test
    void testCreateRecord_success() {
        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(categoryRepo.findById(100)).thenReturn(Optional.of(mockCat));
        when(expenseRepo.save(any())).thenReturn(mockRecord);

        ExpenseRecord input = new ExpenseRecord();
        input.setCategory(mockCat);

        ExpenseRecord saved = service.createRecord(1, input);

        assertNotNull(saved);
        verify(achievementService).checkFirstExpense(1);
        verify(achievementService).checkTenRecords(1);
        verify(expenseRepo).save(any());
    }

    @Test
    void testCreateRecord_categoryNotFound() {
        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(categoryRepo.findById(100)).thenReturn(Optional.empty());

        ExpenseRecord rec = new ExpenseRecord();
        Category c = new Category();
        c.setCategoryId(100);
        rec.setCategory(c);

        assertThrows(RuntimeException.class, () -> service.createRecord(1, rec));
    }

    @Test
    void testUpdateRecord_success() {
        ExpenseRecord updated = new ExpenseRecord();
        updated.setAmount(BigDecimal.valueOf(999));
        updated.setCurrency("AUD");
        updated.setDescription("updated");
        updated.setNotes("notes");
        updated.setPaymentMethod("Card");
        updated.setIsRecurring(true);

        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findById(10)).thenReturn(Optional.of(mockRecord));
        when(expenseRepo.save(any())).thenReturn(mockRecord);

        ExpenseRecord result = service.updateRecord(1, 10, updated);
        assertEquals(mockRecord, result);
        verify(expenseRepo).save(mockRecord);
    }

    @Test
    void testUpdateRecord_unauthorized() {
        User other = new User();
        other.setUser_id(2);
        mockRecord.setUser(other);

        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findById(10)).thenReturn(Optional.of(mockRecord));

        assertThrows(RuntimeException.class, () -> service.updateRecord(1, 10, new ExpenseRecord()));
    }

    @Test
    void testUpdateRecord_notFound() {
        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findById(10)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateRecord(1, 10, new ExpenseRecord()));
    }

    @Test
    void testDeleteRecord_success() {
        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findById(10)).thenReturn(Optional.of(mockRecord));

        service.deleteRecord(1, 10);
        verify(expenseRepo).deleteById(10);
    }

    @Test
    void testDeleteRecord_unauthorized() {
        User other = new User();
        other.setUser_id(2);
        mockRecord.setUser(other);

        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findById(10)).thenReturn(Optional.of(mockRecord));

        assertThrows(RuntimeException.class, () -> service.deleteRecord(1, 10));
    }

    @Test
    void testDeleteRecord_notFound() {
        when(userRepo.findById(1)).thenReturn(Optional.of(mockUser));
        when(expenseRepo.findById(10)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deleteRecord(1, 10));
    }

    @Test
    void testSearch_noKeyword() {
        when(expenseRepo.searchNoKeyword(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        var result = service.search(1, LocalDate.now(), LocalDate.now(), 1, null, false,
                PageRequest.of(0, 5));
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearch_withKeyword() {
        when(expenseRepo.searchWithKeyword(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());
        var result = service.search(1, LocalDate.now(), LocalDate.now(), 1, "coffee", false,
                PageRequest.of(0, 5));
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetReports_allTypes() {
        when(expenseRepo.getWeeklyReportFor(1, 2024, 1)).thenReturn(List.of());
        when(expenseRepo.getMonthlyReportFor(1, 2024, 2)).thenReturn(List.of());
        when(expenseRepo.getYearlyReportFor(1, 2024)).thenReturn(List.of());

        service.getWeeklyReport(1, 2024, 1);
        service.getMonthlyReport(1, 2024, 2);
        service.getYearlyReport(1, 2024);

        verify(expenseRepo).getWeeklyReportFor(1, 2024, 1);
        verify(expenseRepo).getMonthlyReportFor(1, 2024, 2);
        verify(expenseRepo).getYearlyReportFor(1, 2024);
    }

    private ExpenseReportDTO makeReport(int year, Integer period, String name, double amt) {
        return new ExpenseReportDTO(year, period, name, BigDecimal.valueOf(amt));
    }

    @Test
    void testExportReportPdf_weekly_success() {
        when(expenseRepo.getWeeklyReportFor(1, 2024, 10))
                .thenReturn(List.of(makeReport(2024, 10, "Food", 12.3)));

        byte[] pdf = service.exportReportPdf(1, "weekly", 2024, null, 10);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testExportReportPdf_monthly_success() {
        when(expenseRepo.getMonthlyReportFor(1, 2024, 5))
                .thenReturn(List.of(makeReport(2024, 5, "Travel", 99)));

        byte[] pdf = service.exportReportPdf(1, "monthly", 2024, 5, null);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testExportReportPdf_yearly_success() {
        when(expenseRepo.getYearlyReportFor(1, 2024))
                .thenReturn(List.of(makeReport(2024, null, "General", 10)));

        byte[] pdf = service.exportReportPdf(1, "yearly", 2024, null, null);
        assertTrue(pdf.length > 0);
    }

    @Test
    void testExportReportPdf_missingParams() {
        // weekly missing params
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReportPdf(1, "weekly", null, null, 2));
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReportPdf(1, "weekly", 2024, null, null));

        // monthly missing params
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReportPdf(1, "monthly", null, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReportPdf(1, "monthly", 2024, null, null));

        // yearly missing params
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReportPdf(1, "yearly", null, null, null));
    }

    @Test
    void testExportReportPdf_invalidPeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> service.exportReportPdf(1, "unknown", 2024, 1, 1));
    }

    @Test
    void testExportReportPdf_exceptionDuringPdf() {
        when(expenseRepo.getYearlyReportFor(1, 2024))
                .thenReturn(List.of(makeReport(2024, null, "Err", 10)));

        assertThrows(RuntimeException.class,
                () -> service.exportReportPdf(1, "\u0000", 2024, null, null));
    }
}
