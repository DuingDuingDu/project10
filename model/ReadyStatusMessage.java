package com.hoit.checkers.model;

public class ReadyStatusMessage {
    private String roomId;
    private String username; // username으로 변경
    private boolean ready;

    // 기본 생성자
    public ReadyStatusMessage() {
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
