package com.hoit.checkers.dto;

public class PromotionRequest {
    private String roomId;
    private String nickname;

    // 기본 생성자
    public PromotionRequest() {}

    // Getter와 Setter
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}