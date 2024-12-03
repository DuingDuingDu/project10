package com.hoit.checkers.model;

public class RoomJoinMessage {
    private String roomId;
    private String username; // username으로 변경

    // 기본 생성자
    public RoomJoinMessage() {
    }

    // Getter 및 Setter 메서드

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
}
