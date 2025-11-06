package com.example.backend.config.gemini;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


class GeminiHttpClientTest {

    private GeminiHttpClient newClient() {
        GeminiProperties props = new GeminiProperties();
        props.setApiKey("test-key");
        props.setModel("gemini-2.5-flash");
        props.setTimeoutMillis(5000);
        return new GeminiHttpClient(props);
    }

    @Test
    void generateAdviceJson_success_returnsInnerJson() {
        String inner = "{\"summary\":\"hello\",\"bullets\":[{\"title\":\"A\",\"detail\":\"B\"}]}";
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + inner.replace("\"", "\\\"") + "\"}]}}]}";

        try (MockedConstruction<RestTemplate> ignored = Mockito.mockConstruction(
                RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(ResponseEntity.ok(body))
        )) {
            GeminiHttpClient client = newClient();

            String result = client.generateAdviceJson(
                    "2025-10",
                    "AUD",
                    new BigDecimal("100.00"),
                    List.of(Map.of("cat", "Food", "amount", 50, "pct", 50)),
                    "en-AU"
            );

            assertEquals(inner, result, "The JSON string within candidates[0].content.parts[0].text should be returned (unmodified).");
        }
    }

    @Test
    void generateAdviceJson_badInnerText_throwsRuntimeException() {
        String badInner = "NOT_JSON";
        String body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + badInner + "\"}]}}]}";

        try (MockedConstruction<RestTemplate> ignored = Mockito.mockConstruction(
                RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(ResponseEntity.ok(body))
        )) {
            GeminiHttpClient client = newClient();

            RuntimeException ex = assertThrows(RuntimeException.class, () ->
                    client.generateAdviceJson("2025-10", "AUD", BigDecimal.TEN, List.of(), "en")
            );
            assertTrue(ex.getMessage().toLowerCase().contains("parse"), "should include parse / Gemini parse failed");
        }
    }
}
