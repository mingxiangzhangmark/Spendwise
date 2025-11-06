package com.example.backend.service;

import com.example.backend.config.gemini.GeminiClient;
import com.example.backend.model.AiRecommendation;
import com.example.backend.model.FeatureSnapshot;
import com.example.backend.repository.AiRecommendationRepository;
import com.example.backend.repository.ExpenseRecordRepository;
import com.example.backend.repository.FeatureSnapshotRepository;
import com.example.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiAdviceService {

    private final ExpenseRecordRepository expenseRepo;
    private final AiRecommendationRepository recRepo;
    private final FeatureSnapshotRepository snapshotRepo;
    private final UserRepository userRepo;
    private final GeminiClient gemini;
    private final ObjectMapper om = new ObjectMapper();

    private static final String CURRENCY = "AUD";

    public Map<String,Object> generate(Integer userId, YearMonth ym, String languageTag) {
        int year = ym.getYear();
        int month = ym.getMonthValue();
        var rows = expenseRepo.findMonthlySpend(userId, year, month);

        BigDecimal total = rows.stream()
                .map(ExpenseRecordRepository.CategorySpend::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.signum() == 0) {
            return Map.of(
                    "month", ym.toString(),
                    "language", languageTag,
                    "noData", true,
                    "message", "Insufficient transactions this month to generate suggestions."
            );
        }

        List<Map<String, Object>> totals = new ArrayList<>();
        for (var r : rows) {
            BigDecimal pct = r.getAmount().divide(total, 4, java.math.RoundingMode.HALF_UP);
            totals.add(Map.of(
                    "catId", r.getCategoryId(),
                    "catName", r.getCategoryName(),
                    "amount", r.getAmount(),
                    "pct", pct
            ));
        }

        var user = userRepo.findById(userId).orElseThrow();
        var monthStr = ym.toString();
        var totalsJson = writeJson(totals);
        var snap = snapshotRepo.findByUserAndMonth(userRepo.findById(userId).orElseThrow(), monthStr)
                .orElseGet(FeatureSnapshot::new);
        snap.setUser(user);
        snap.setMonth(monthStr);
        snap.setTotalsByCategoryJson(totalsJson);
        snap.setTotalSpending(total);
        snap.setCurrency(CURRENCY);
        snapshotRepo.save(snap);

        String llmJson;
        try {
            llmJson = gemini.generateAdviceJson(monthStr, CURRENCY, total, totals, languageTag);
        } catch (Exception ex) {
            llmJson = """
                {"summary":"System is busy. Generated brief suggestions using rules.",
                 "bullets":[
                   {"title":"Set limits for high-spending categories","detail":"Start with a fixed weekly budget"},
                   {"title":"Schedule regular reviews","detail":"Review non-essential spending weekly"}
                 ]}""";
        }

        Map<String,Object> content = readJson(llmJson);
        content.put("month", monthStr);
        content.put("language", languageTag);
        content.put("currency", CURRENCY);
        content.put("totalSpending", total);
        content.put("totalsByCategory", totals);

        String json = writeJson(content);

        var rec = recRepo.findByUserAndMonth(userRepo.findById(userId).orElseThrow(), monthStr)
                .orElseGet(AiRecommendation::new);
        rec.setUser(user);
        rec.setMonth(monthStr);
        rec.setLanguage(languageTag);
        rec.setContent(json);
        recRepo.save(rec);

        return content;
    }

    private String writeJson(Object src) {
        try { return om.writeValueAsString(src); }
        catch (Exception e) { throw new RuntimeException("JSON_ENCODE_FAILED", e); }
    }
    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        try { return om.readValue(json, Map.class); }
        catch (Exception e) { throw new RuntimeException("JSON_DECODE_FAILED", e); }
    }
}
