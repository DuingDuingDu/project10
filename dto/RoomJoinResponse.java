package com.hoit.checkers.dto;

public class RoomJoinResponse {
    private String nickname;
    private String userType;
    private String roomId;

    public RoomJoinResponse(String nickname, String userType, String roomId) {
        this.nickname = nickname;
        this.userType = userType;
        this.roomId = roomId;
    }

    // Getters and Setters
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
