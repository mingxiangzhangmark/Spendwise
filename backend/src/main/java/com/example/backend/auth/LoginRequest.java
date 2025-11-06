package com.example.backend.auth;

public class LoginRequest {
    private String identifier;
    private String password;

    public String getIdentifier() {
        return identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // getters/setters
}