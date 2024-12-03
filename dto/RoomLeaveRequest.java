// RoomLeaveRequest.java

package com.hoit.checkers.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RoomLeaveRequest {
    @JsonProperty("roomId") // 클라이언트와 일치하도록 수정
    private String roomId;

    @JsonProperty("username")
    private String username;

    // 기본 생성자
    public RoomLeaveRequest() {}

    // Getters 및 Setters
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
