package com.hoit.checkers.model;

public class GameStartResponse {
    private String message;

    // 기본 생성자
    public GameStartResponse() {
    }

    // 생성자
    public GameStartResponse(String message) {
        this.message = message;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
