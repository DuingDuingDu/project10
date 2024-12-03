package com.hoit.checkers.model;

public class LobbyUser {
    private String uniqueId; // sessionId 또는 userId
    private String nickname;
    private boolean isMember; // 회원 여부

    // 기본 생성자
    public LobbyUser() {}

    // 생성자
    public LobbyUser(String uniqueId, String nickname, boolean isMember) {
        this.uniqueId = uniqueId;
        this.nickname = nickname;
        this.isMember = isMember;
    }

    // Getters and Setters
    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public boolean isMember() { return isMember; }
    public void setMember(boolean isMember) { this.isMember = isMember; }
}
