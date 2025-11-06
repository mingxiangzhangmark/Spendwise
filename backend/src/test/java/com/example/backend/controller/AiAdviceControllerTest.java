package com.example.backend.controller;

import com.example.backend.dto.UserDTO;
import com.example.backend.service.AiAdviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAdviceControllerTest {

    @Mock
    private AiAdviceService service;

    private AiAdviceController controller;

    @BeforeEach
    void setUp() {
        controller = new AiAdviceController(service);
    }

    @Test
    void generateWithExplicitMonthUsesParsedYearMonth() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER", new UserDTO(8, "rose"));

        Map<String, Object> payload = Map.of("status", "ok");
        YearMonth target = YearMonth.parse("2024-11");
        when(service.generate(8, target, "en-US")).thenReturn(payload);

        var response = controller.generate("2024-11", session);

        assertThat(response).isSameAs(payload);
    }

    @Test
    void generateWithoutMonthFallsBackToCurrentMonth() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER", new UserDTO(3, "mike"));

        Map<String, Object> payload = Map.of("status", "fallback");
        when(service.generate(eq(3), any(YearMonth.class), eq("en-US"))).thenReturn(payload);

        YearMonth expected = YearMonth.now();
        var response = controller.generate("  ", session);

        assertThat(response).isSameAs(payload);

        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        verify(service).generate(eq(3), captor.capture(), eq("en-US"));
        assertThat(captor.getValue()).isEqualTo(expected);
    }

    @Test
    void generateWithNullMonthAlsoFallsBackToCurrentMonth() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("USER", new UserDTO(4, "jane"));

        Map<String, Object> payload = Map.of("status", "null-month");
        when(service.generate(eq(4), any(YearMonth.class), eq("en-US"))).thenReturn(payload);

        YearMonth expected = YearMonth.now();
        var response = controller.generate(null, session);

        assertThat(response).isSameAs(payload);

        ArgumentCaptor<YearMonth> captor = ArgumentCaptor.forClass(YearMonth.class);
        verify(service).generate(eq(4), captor.capture(), eq("en-US"));
        assertThat(captor.getValue()).isEqualTo(expected);
    }
}
