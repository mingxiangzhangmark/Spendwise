package com.example.backend.controller;

import com.example.backend.dto.UserDTO;
import com.example.backend.service.AiAdviceService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Locale;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AiAdviceController {

    private final AiAdviceService service;

    @PostMapping("/suggestions/generate")
    public Map<String, Object> generate(@RequestParam(required=false) String month,
                                        HttpSession session) {
//        {
//    "summary": "本月总支出为15.70澳元，其中餐饮和交通是主要的开销。",
//    "bullets": [
//        {
//            "title": "餐饮支出分析",
//            "detail": "本月餐饮支出占比近八成，建议回顾餐饮消费习惯，考虑是否有优化空间，例如自制餐食或选择更经济的用餐方式。"
//        },
//        {
//            "title": "交通支出评估",
//            "detail": "交通支出占比较小，可以审视当前的出行方式是否高效和经济，若有固定通勤需求，可考虑是否有更具成本效益的替代方案。"
//        }
//    ],
//    "month": "2025-10",
//    "language": "zh-CN",
//    "currency": "AUD",
//    "totalSpending": 15.70,
//    "totalsByCategory": [
//        {
//            "amount": 12.50,
//            "catId": 9,
//            "catName": "Food",
//            "pct": 0.7962
//        },
//        {
//            "amount": 3.20,
//            "catId": 10,
//            "catName": "Transport",
//            "pct": 0.2038
//        }
//    ]
//}
        Integer userId = ((UserDTO) session.getAttribute("USER")).getId();
        YearMonth ym = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
        String languageTag = Locale.US.toLanguageTag();
        return service.generate(userId, ym, languageTag);
    }
}
