package com.example.backend.config.gemini;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiPropertiesTest {

    @Test
    void gettersSetters_work() {
        GeminiProperties props = new GeminiProperties();
        props.setApiKey("secret-key");
        props.setModel("gemini-2.5-flash");
        props.setTimeoutMillis(8000);

        assertEquals("secret-key", props.getApiKey());
        assertEquals("gemini-2.5-flash", props.getModel());
        assertEquals(8000, props.getTimeoutMillis());
    }
}
