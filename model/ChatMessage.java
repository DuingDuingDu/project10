package com.hoit.checkers.model;

public class ChatMessage {
    private String sender;
    private String content;
    private MessageType type;
    private String roomId; // 방 ID 추가
    //private String type;


    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }

    // 기본 생성자
    public ChatMessage() {
    }

    // 생성자
    public ChatMessage(String sender, String content, MessageType type) {
        this.sender = sender;
        this.content = content;
        this.type = type;
    }

    // Getter와 Setter
    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
