package com.example.uninest.model;

public class RegisterTokenRequest {
    private String token;

    public RegisterTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}