package com.example.backend.config.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
public class GeminiHttpClient implements GeminiClient {

    private final GeminiProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private RestTemplate newRt() {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(props.getTimeoutMillis());
        f.setReadTimeout(props.getTimeoutMillis());
        return new RestTemplate(f);
    }

    @Override
    public String generateAdviceJson(String month,
                                     String currency,
                                     BigDecimal totalSpending,
                                     List<Map<String, Object>> totalsByCategory,
                                     String languageTag) {

        String prompt = """
            你是个人理财助手。请仅输出 JSON（不要任何说明文字），模式如下：
            {
              "summary": "一句话概括本月支出情况（用%s写）",
              "bullets": [
                {"title":"要点1（简短）","detail":"可执行建议"},
                {"title":"要点2","detail":"可执行建议（最多5条）"}
              ]
            }
            约束：不要出现用户个人信息；不要捏造输入里没有的事实；语气中性、简明可执行。
            输入数据（仅为聚合）：
            month=%s, currency=%s, totalSpending=%s,
            totalsByCategory=%s
            """.formatted(languageTag, month, currency, totalSpending, toInlineJson(totalsByCategory));

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                props.getModel() + ":generateContent?key=" + props.getApiKey();

        System.out.println("url: "+ props.getApiKey());

        var rt = newRt();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var req = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = rt.exchange(url, HttpMethod.POST, req, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("Gemini request failed: " + resp.getStatusCode());
        }

        try {
            JsonNode root = om.readTree(resp.getBody());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new RuntimeException("No candidates returned by Gemini");
            }
            JsonNode textNode = candidates.get(0)
                    .path("content").path("parts").get(0).path("text");
            String json = textNode.asText();
            om.readTree(json);
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Gemini parse failed", e);
        }
    }

    private String toInlineJson(Object o) {
        try { return om.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }
}
