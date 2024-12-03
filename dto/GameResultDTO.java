package com.hoit.checkers.dto;

public class GameResultDTO {
    private String roomId;
    private String winnerNickname;
    private String loserNickname;
    private boolean isDraw;

    // 기본 생성자
    public GameResultDTO() {
    }

    // Getter 및 Setter 메서드

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getWinnerNickname() {
        return winnerNickname;
    }

    public void setWinnerNickname(String winnerNickname) {
        this.winnerNickname = winnerNickname;
    }

    public String getLoserNickname() {
        return loserNickname;
    }

    public void setLoserNickname(String loserNickname) {
        this.loserNickname = loserNickname;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public void setDraw(boolean draw) {
        isDraw = draw;
    }
}
