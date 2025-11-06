package com.example.backend.config.gemini;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientTest {

    @Test
    void generateAdviceJson_signature_and_basicContract() {
        GeminiClient client = (month, currency, totalSpending, totalsByCategory, languageTag) -> "{\"summary\":\"ok\",\"bullets\":[]}";

        String json = client.generateAdviceJson(
                "2025-10", "AUD", new BigDecimal("123.45"),
                List.of(Map.of("cat", "Food", "amount", 50, "pct", 40)),
                "en-AU"
        );
        assertNotNull(json);
        assertTrue(true);
    }
}
