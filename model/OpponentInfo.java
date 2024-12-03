package com.hoit.checkers.model;

public class OpponentInfo {
    private String nickname;
    private int wins;
    private int draws;
    private int losses;
    private double winRate; 
    private String opponentNickname;
    private UserRole role;  // 추가된 필드

    // 기존 생성자 유지
    public OpponentInfo(String nickname, int wins, int draws, int losses, double winRate) {
        this.nickname = nickname;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.winRate = winRate;
    }

    // 새로운 생성자 추가
    public OpponentInfo(String nickname, int wins, int draws, int losses, double winRate, UserRole role) {
        this.nickname = nickname;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.winRate = winRate;
        this.role = role;
    }

    // getter, setter 추가
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}