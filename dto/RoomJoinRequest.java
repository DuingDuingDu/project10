package com.hoit.checkers.dto;

public class RoomJoinRequest {
    private String roomId;
    private String password;
    private String nickname;

    // 기본 생성자
    public RoomJoinRequest() {
    }

    // 생성자
    public RoomJoinRequest(String roomId, String password, String nickname) {
        this.roomId = roomId;
        this.password = password;
        this.nickname = nickname;
    }

    // Getters and Setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
