package com.hoit.checkers.dto;

public class RoomCreationRequest {
    private String name;
    private int maxUsers;
    private String password;
    private String createdByNickname;
    private String nickname; // 방 생성 시 참여할 사용자 닉네임
    private boolean privateRoom; // 비공개 방 여부 추가

    // 기본 생성자
    public RoomCreationRequest() {
    }

    // 모든 필드를 포함한 생성자 (선택 사항)
    public RoomCreationRequest(String name, int maxUsers, String password, String createdByNickname, String nickname, boolean privateRoom) {
        this.name = name;
        this.maxUsers = maxUsers;
        this.password = password;
        this.createdByNickname = createdByNickname;
        this.nickname = nickname;
        this.privateRoom = privateRoom;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(int maxUsers) {
        this.maxUsers = maxUsers;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCreatedByNickname() {
        return createdByNickname;
    }

    public void setCreatedByNickname(String createdByNickname) {
        this.createdByNickname = createdByNickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isPrivateRoom() {
        return privateRoom;
    }

    public void setPrivateRoom(boolean privateRoom) {
        this.privateRoom = privateRoom;
    }
}
