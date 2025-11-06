package com.example.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CorsConfigTest.DummyController.class)
@Import(CorsConfig.class)
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    static class DummyController {
        @GetMapping("/hello")
        public String hello() { return "hi"; }
    }

    @Test
    void preflight_allowsConfiguredOriginAndMethods() throws Exception {
        mockMvc.perform(
                        options("/hello")
                                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type")
                )
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }
}
