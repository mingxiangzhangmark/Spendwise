package com.example.backend.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    void ctorNoArg_and_setter_getter() {
        LoginResponse resp = new LoginResponse();
        resp.setMessage("OK");
        assertEquals("OK", resp.getMessage());
    }

    @Test
    void ctorWithMessage_setsField() {
        LoginResponse resp = new LoginResponse("Login successful");
        assertEquals("Login successful", resp.getMessage());
    }
}
