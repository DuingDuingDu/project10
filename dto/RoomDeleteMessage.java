package com.hoit.checkers.dto;

public class RoomDeleteMessage {
    private String roomId;

    public RoomDeleteMessage() {
    }

    public RoomDeleteMessage(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
