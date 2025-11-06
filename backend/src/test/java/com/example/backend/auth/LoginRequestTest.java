package com.example.backend.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void gettersAndSetters_work() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("user@example.com");
        req.setPassword("p@ssw0rd");

        assertEquals("user@example.com", req.getIdentifier());
        assertEquals("p@ssw0rd", req.getPassword());
    }
}

