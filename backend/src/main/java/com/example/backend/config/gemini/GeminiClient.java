package com.example.backend.config.gemini;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface GeminiClient {
    /**
     * @return 形如：{"summary":"...","bullets":[{"title":"..","detail":".."},...]}
     */
    String generateAdviceJson(String month,
                              String currency,
                              BigDecimal totalSpending,
                              List<Map<String,Object>> totalsByCategory,
                              String languageTag);
}
