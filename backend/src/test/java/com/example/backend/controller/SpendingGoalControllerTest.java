package com.example.backend.controller;

import com.example.backend.dto.CreateSpendingGoalRequest;
import com.example.backend.dto.SpendingGoalProgressDTO;
import com.example.backend.dto.SpendingGoalResponse;
import com.example.backend.model.GoalPeriod;
import com.example.backend.model.User;
import com.example.backend.security.SessionUserResolver;
import com.example.backend.service.SpendingGoalService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpendingGoalControllerTest {

    @Mock
    private SpendingGoalService goalService;

    @Mock
    private SessionUserResolver sessionUserResolver;

    @Mock
    private HttpSession session;

    private SpendingGoalController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new SpendingGoalController(goalService, sessionUserResolver);
        user = new User();
        user.setUser_id(44);
        when(sessionUserResolver.getCurrentUser(session)).thenReturn(user);
    }

    @Test
    void listActiveGoalsReturnsServiceData() {
        SpendingGoalResponse response = new SpendingGoalResponse();
        response.setGoalId(1L);
        response.setCategoryId(2);
        response.setTargetAmount(BigDecimal.TEN);

        when(goalService.listActiveGoals(user)).thenReturn(List.of(response));

        ResponseEntity<List<SpendingGoalResponse>> result = controller.listActiveGoals(session);

        assertThat(result.getBody()).containsExactly(response);
    }

    @Test
    void createGoalDelegatesToService() {
        CreateSpendingGoalRequest request = new CreateSpendingGoalRequest();
        request.setCategoryId(3);
        request.setPeriod(GoalPeriod.MONTHLY);
        request.setTargetAmount(BigDecimal.valueOf(500));
        request.setStartDate(LocalDate.now());

        SpendingGoalResponse created = new SpendingGoalResponse();
        created.setGoalId(9L);
        created.setTargetAmount(BigDecimal.valueOf(500));

        when(goalService.createGoal(user, request)).thenReturn(created);

        ResponseEntity<SpendingGoalController.ApiMessage<SpendingGoalResponse>> response =
                controller.createGoal(session, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .contains("Goal created successfully");
        assertThat(response.getBody().getData()).isEqualTo(created);

        verify(goalService).createGoal(user, request);
    }

    @Test
    void listProgressReturnsDtos() {
        SpendingGoalProgressDTO dto = new SpendingGoalProgressDTO(
                1L, "Food", "MONTHLY",
                LocalDate.now(), LocalDate.now().plusMonths(1),
                BigDecimal.valueOf(200), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), 50.0, 10, "ON_TRACK", "NONE", 80, 100
        );

        when(goalService.listProgressForActiveGoals(user)).thenReturn(List.of(dto));

        List<SpendingGoalProgressDTO> result = controller.listProgress(session);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getProgressReturnsSingleDto() {
        SpendingGoalProgressDTO dto = new SpendingGoalProgressDTO(
                22L, "Transport", "WEEKLY", LocalDate.now(), LocalDate.now().plusWeeks(1),
                BigDecimal.valueOf(50), BigDecimal.valueOf(25),
                BigDecimal.valueOf(25), 50.0, 4, "ON_TRACK", "NONE", 80, 100
        );

        when(goalService.getProgressForGoal(22L, user)).thenReturn(dto);

        SpendingGoalProgressDTO result = controller.getProgress(22L, session);

        assertThat(result).isEqualTo(dto);
    }
}
