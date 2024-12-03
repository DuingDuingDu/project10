package com.hoit.checkers.model;

public class GameStartMessage {
    private String roomId;

    // 기본 생성자
    public GameStartMessage() {
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
