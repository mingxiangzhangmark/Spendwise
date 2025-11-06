package com.example.backend.controller;

import com.example.backend.dto.ExpenseRecordDTO;
import com.example.backend.dto.ExpenseReportDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.model.Category;
import com.example.backend.model.ExpenseRecord;
import com.example.backend.model.FeatureSnapshot;
import com.example.backend.model.RecurringExpenseSchedule;
import com.example.backend.model.User;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.FeatureSnapshotRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.ExpenseRecordService;
import com.example.backend.service.RecurringExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRecordControllerTest {

    @Mock
    private ExpenseRecordService recordService;

    @Mock
    private RecurringExpenseService recurringExpenseService;

    @Mock
    private ExpenseRecordRepository expenseRecordRepository;

    @Mock
    private FeatureSnapshotRepository featureSnapshotRepository;

    @Mock
    private UserRepository userRepository;

    private ExpenseRecordController controller;

    private User user;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        controller = new ExpenseRecordController(
                recordService,
                recurringExpenseService,
                expenseRecordRepository,
                userRepository,
                featureSnapshotRepository
        );
        user = buildUser(5, "bob");
        session = new MockHttpSession();
        session.setAttribute("USER", new UserDTO(5, "bob"));
    }

    @Test
    void getSnapshotReturnsDtoWhenFound() {
        FeatureSnapshot snapshot = new FeatureSnapshot();
        snapshot.setSnapshotId(1L);
        snapshot.setUser(user);
        snapshot.setMonth("2024-10");
        snapshot.setCurrency("AUD");
        snapshot.setTotalSpending(BigDecimal.TEN);
        snapshot.setTotalsByCategoryJson("[]");

        when(userRepository.getReferenceById(5)).thenReturn(user);
        when(featureSnapshotRepository.findByUserAndMonth(user, "2024-10"))
                .thenReturn(Optional.of(snapshot));

        var dto = controller.getSnapshot(session, "2024-10");

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCurrency()).isEqualTo("AUD");
        assertThat(dto.getTotalSpending()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void getSnapshotThrowsWhenMissing() {
        when(userRepository.getReferenceById(5)).thenReturn(user);
        when(featureSnapshotRepository.findByUserAndMonth(user, "2024-11"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getSnapshot(session, "2024-11"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("snapshot not found");
    }

    @Test
    void getRecordsMapsEntitiesToDtos() {
        ExpenseRecord record = buildExpenseRecord(10, false);
        when(recordService.getRecordsForUser(5)).thenReturn(List.of(record));

        ResponseEntity<List<ExpenseRecordDTO>> response = controller.getRecords(session);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getExpenseId()).isEqualTo(10L);
        assertThat(response.getBody().get(0).getIsRecurring()).isFalse();
    }

    @Test
    void searchConvertsParametersAndDelegates() {
        ExpenseRecord record = buildExpenseRecord(11, true);
        Page<ExpenseRecord> page = new PageImpl<>(List.of(record));
        when(recordService.search(eq(5), any(), any(), eq(3), anyString(), eq(true), any(Pageable.class)))
                .thenReturn(page);

        var response = controller.search(
                session,
                "2024-01-01",
                "2024-01-31",
                3,
                "coffee",
                true,
                0,
                5,
                "amount",
                "asc"
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(recordService).search(eq(5), eq(LocalDate.parse("2024-01-01")),
                eq(LocalDate.parse("2024-01-31")), eq(3),
                eq("coffee"), eq(true), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("amount").getDirection().isAscending()).isTrue();
    }

    @Test
    void searchDefaultsToDescSortWhenDirectionNotAsc() {
        ExpenseRecord record = buildExpenseRecord(12, false);
        when(recordService.search(eq(5), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        var response = controller.search(
                session, null, null, null, null, null, 1, 3, "expenseDate", "desc"
        );

        assertThat(response.getBody()).isNotNull();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(recordService).search(eq(5), eq(null), eq(null), eq(null),
                eq(null), eq(null), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("expenseDate").getDirection().isDescending()).isTrue();
    }

    @Test
    void createRecordForRecurringExpenseTriggersScheduleCreation() {
        ExpenseRecord request = buildExpenseRecord(null, true);
        ExpenseRecord created = buildExpenseRecord(13, true);
        RecurringExpenseSchedule schedule = created.getRecurringSchedule();
        if (schedule == null) {
            schedule = new RecurringExpenseSchedule();
            schedule.setId(9);
            schedule.setFrequency(RecurringExpenseSchedule.Frequency.MONTHLY);
            created.setRecurringSchedule(schedule);
        }

        when(recordService.createRecord(5, request)).thenReturn(created);

        var response = controller.createRecord(request, "monthly", session);

        verify(recurringExpenseService).onManualExpenseSaved(created,
                RecurringExpenseSchedule.Frequency.MONTHLY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRecurringScheduleId()).isEqualTo(created.getRecurringSchedule().getId());
    }

    @Test
    void createRecordNonRecurringSkipsRecurringHandling() {
        ExpenseRecord request = buildExpenseRecord(null, false);
        ExpenseRecord created = buildExpenseRecord(14, false);

        when(recordService.createRecord(5, request)).thenReturn(created);

        var response = controller.createRecord(request, null, session);

        verify(recurringExpenseService, never()).onManualExpenseSaved(any(), any());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsRecurring()).isFalse();
    }

    @Test
    void createRecordRecurringWithNullFrequencySkipsRecurringHandling() {
        ExpenseRecord request = buildExpenseRecord(null, true);
        ExpenseRecord created = buildExpenseRecord(40, true);

        when(recordService.createRecord(5, request)).thenReturn(created);

        var response = controller.createRecord(request, null, session);

        verify(recurringExpenseService, never()).onManualExpenseSaved(any(), any());
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void createRecordRecurringWithBlankFrequencySkipsRecurringHandling() {
        ExpenseRecord request = buildExpenseRecord(null, true);
        ExpenseRecord created = buildExpenseRecord(41, true);

        when(recordService.createRecord(5, request)).thenReturn(created);

        var response = controller.createRecord(request, "   ", session);

        verify(recurringExpenseService, never()).onManualExpenseSaved(any(), any());
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateRecordReturnsImmediatelyWhenNewRecurringNull() {
        ExpenseRecord before = buildExpenseRecord(15, false);
        ExpenseRecord request = buildExpenseRecord(15, false);
        request.setIsRecurring(null);
        ExpenseRecord saved = buildExpenseRecord(15, false);

        when(expenseRecordRepository.findById(15)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 15, request)).thenReturn(saved);

        var response = controller.updateRecord(15, request, null, session);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getExpenseId()).isEqualTo(15L);
    }

    @Test
    void updateRecordNonRecurringToRecurringWithoutFrequencyThrows() {
        ExpenseRecord before = buildExpenseRecord(16, false);
        ExpenseRecord updated = buildExpenseRecord(16, true);

        when(expenseRecordRepository.findById(16)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 16, updated)).thenReturn(updated);

        assertThatThrownBy(() -> controller.updateRecord(16, updated, null, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("frequency required");
    }

    @Test
    void updateRecordNonRecurringToRecurringSchedulesNewPlan() {
        ExpenseRecord before = buildExpenseRecord(17, false);
        ExpenseRecord updated = buildExpenseRecord(17, true);

        when(expenseRecordRepository.findById(17)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 17, updated)).thenReturn(updated);

        var response = controller.updateRecord(17, updated, "weekly", session);

        verify(recurringExpenseService).onManualExpenseSaved(updated,
                RecurringExpenseSchedule.Frequency.WEEKLY);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateRecordRecurringToNonRecurringCancelsSchedule() {
        ExpenseRecord before = buildExpenseRecord(18, true);
        before.getRecurringSchedule().setId(30);
        ExpenseRecord updated = buildExpenseRecord(18, false);

        when(expenseRecordRepository.findById(18)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 18, updated)).thenReturn(updated);

        var response = controller.updateRecord(18, updated, null, session);

        verify(recurringExpenseService).cancelSchedule(30);
        verify(expenseRecordRepository).save(updated);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateRecordRecurringToRecurringChangesFrequencyWhenDifferent() {
        ExpenseRecord before = buildExpenseRecord(19, true);
        before.getRecurringSchedule().setId(31);
        before.getRecurringSchedule().setFrequency(RecurringExpenseSchedule.Frequency.MONTHLY);

        ExpenseRecord updated = buildExpenseRecord(19, true);

        when(expenseRecordRepository.findById(19)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 19, updated)).thenReturn(updated);

        var response = controller.updateRecord(19, updated, "weekly", session);

        verify(recurringExpenseService).cancelSchedule(31);
        verify(recurringExpenseService).onManualExpenseSaved(updated,
                RecurringExpenseSchedule.Frequency.WEEKLY);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateRecordRecurringCreatesScheduleWhenMissing() {
        ExpenseRecord before = buildExpenseRecord(50, true);
        before.setRecurringSchedule(null);
        ExpenseRecord request = buildExpenseRecord(50, true);
        request.setRecurringSchedule(null);
        ExpenseRecord saved = buildExpenseRecord(50, true);
        saved.setRecurringSchedule(null);

        when(expenseRecordRepository.findById(50)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 50, request)).thenReturn(saved);

        controller.updateRecord(50, request, "monthly", session);

        verify(recurringExpenseService, never()).cancelSchedule(anyInt());
        verify(recurringExpenseService).onManualExpenseSaved(saved, RecurringExpenseSchedule.Frequency.MONTHLY);
    }

    @Test
    void updateRecordRecurringWithSameFrequencyMakesNoChanges() {
        ExpenseRecord before = buildExpenseRecord(51, true);
        before.getRecurringSchedule().setFrequency(RecurringExpenseSchedule.Frequency.MONTHLY);
        ExpenseRecord request = buildExpenseRecord(51, true);
        ExpenseRecord saved = buildExpenseRecord(51, true);
        saved.getRecurringSchedule().setFrequency(RecurringExpenseSchedule.Frequency.MONTHLY);

        when(expenseRecordRepository.findById(51)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 51, request)).thenReturn(saved);

        controller.updateRecord(51, request, "monthly", session);

        verify(recurringExpenseService, never()).cancelSchedule(anyInt());
        verify(recurringExpenseService, never()).onManualExpenseSaved(any(), any());
    }

    @Test
    void updateRecordRecurringKeepsScheduleWhenFrequencyMissing() {
        ExpenseRecord before = buildExpenseRecord(52, true);
        ExpenseRecord request = buildExpenseRecord(52, true);
        ExpenseRecord saved = buildExpenseRecord(52, true);

        when(expenseRecordRepository.findById(52)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 52, request)).thenReturn(saved);

        ResponseEntity<ExpenseRecordDTO> response = controller.updateRecord(52, request, null, session);

        verify(recurringExpenseService, never()).cancelSchedule(anyInt());
        verify(recurringExpenseService, never()).onManualExpenseSaved(any(), any());
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void updateRecordRecurringToNonRecurringWithoutScheduleSkipsCancel() {
        ExpenseRecord before = buildExpenseRecord(53, true);
        before.setRecurringSchedule(null);
        ExpenseRecord request = buildExpenseRecord(53, false);
        ExpenseRecord saved = buildExpenseRecord(53, false);

        when(expenseRecordRepository.findById(53)).thenReturn(Optional.of(before));
        when(recordService.updateRecord(5, 53, request)).thenReturn(saved);

        controller.updateRecord(53, request, null, session);

        verify(recurringExpenseService, never()).cancelSchedule(anyInt());
    }

    @Test
    void deleteRecordThrowsWhenRecordDoesNotBelongToUser() {
        ExpenseRecord record = buildExpenseRecord(20, false);
        record.getUser().setUser_id(99);

        when(expenseRecordRepository.findById(20)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> controller.deleteRecord(20, false, session))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Not your record");
    }

    @Test
    void deleteRecordCancelsRecurringScheduleWhenRequested() {
        ExpenseRecord record = buildExpenseRecord(21, true);
        record.getRecurringSchedule().setId(41);

        when(expenseRecordRepository.findById(21)).thenReturn(Optional.of(record));

        var response = controller.deleteRecord(21, true, session);

        verify(recurringExpenseService).cancelSchedule(41);
        verify(recordService).deleteRecord(5, 21);
        assertThat(response.getBody()).containsEntry("message", "Record deleted successfully");
    }

    @Test
    void deleteRecordWithoutRecurringReturnsMessage() {
        ExpenseRecord record = buildExpenseRecord(22, false);

        when(expenseRecordRepository.findById(22)).thenReturn(Optional.of(record));

        var response = controller.deleteRecord(22, false, session);

        verify(recurringExpenseService, never()).cancelSchedule(anyInt());
        verify(recordService).deleteRecord(5, 22);
        assertThat(response.getBody()).containsEntry("message", "Record deleted successfully");
    }

    @Test
    void deleteRecordWhenSchedulePresentButCancelFlagFalseDoesNotCancel() {
        ExpenseRecord record = buildExpenseRecord(54, true);

        when(expenseRecordRepository.findById(54)).thenReturn(Optional.of(record));

        controller.deleteRecord(54, false, session);

        verify(recurringExpenseService, never()).cancelSchedule(anyInt());
    }

    @Test
    void searchTreatsBlankDatesAsNull() {
        ExpenseRecord record = buildExpenseRecord(55, false);
        when(recordService.search(eq(5), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        controller.search(session, "  ", "\t", null, null, null, 0, 5, "expenseDate", "asc");

        verify(recordService).search(eq(5), eq(null), eq(null), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    void weeklyReportDelegatesToService() {
        List<ExpenseReportDTO> report = List.of(new ExpenseReportDTO(2024, 1, "Food", BigDecimal.TEN));
        when(recordService.getWeeklyReport(5, 2024, 1)).thenReturn(report);

        ResponseEntity<List<ExpenseReportDTO>> response = controller.getWeeklyReport(2024, 1, session);

        assertThat(response.getBody()).isEqualTo(report);
    }

    @Test
    void monthlyReportDelegatesToService() {
        List<ExpenseReportDTO> report = List.of(new ExpenseReportDTO(2024, 5, "Transport", BigDecimal.ONE));
        when(recordService.getMonthlyReport(5, 2024, 5)).thenReturn(report);

        ResponseEntity<List<ExpenseReportDTO>> response = controller.getMonthlyReport(2024, 5, session);

        assertThat(response.getBody()).isEqualTo(report);
    }

    @Test
    void yearlyReportDelegatesToService() {
        List<ExpenseReportDTO> report = List.of(new ExpenseReportDTO(2024, null, "General", BigDecimal.ZERO));
        when(recordService.getYearlyReport(5, 2024)).thenReturn(report);

        ResponseEntity<List<ExpenseReportDTO>> response = controller.getYearlyReport(2024, session);

        assertThat(response.getBody()).isEqualTo(report);
    }

    private ExpenseRecord buildExpenseRecord(Integer expenseId, boolean recurring) {
        ExpenseRecord record = new ExpenseRecord();
        record.setExpenseId(expenseId);
        record.setUser(buildUser(5, "bob"));
        record.setCategory(buildCategory(6, "Food"));
        record.setAmount(BigDecimal.valueOf(25));
        record.setCurrency("USD");
        record.setExpenseDate(LocalDate.of(2024, 1, 1));
        record.setDescription("Lunch");
        record.setIsRecurring(recurring);
        if (recurring) {
            RecurringExpenseSchedule schedule = new RecurringExpenseSchedule();
            schedule.setId(50);
            schedule.setFrequency(RecurringExpenseSchedule.Frequency.MONTHLY);
            record.setRecurringSchedule(schedule);
        }
        return record;
    }

    private User buildUser(int id, String username) {
        User u = new User();
        u.setUser_id(id);
        u.setUsername(username);
        u.setEmail(username + "@example.com");
        return u;
    }

    private Category buildCategory(int id, String name) {
        Category category = new Category();
        category.setCategoryId(id);
        category.setCategoryName(name);
        return category;
    }
}
