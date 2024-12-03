package com.hoit.checkers.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hoit.checkers.dto.GameResultDTO;

@Entity
@Table(name = "gameResults")
public class GameResult implements Serializable {

    private static final long serialVersionUID = 1L;

    // 게임 결과 (WIN, LOSE, DRAW)
    public enum ResultType {
        WIN,
        LOSE,
        DRAW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player1Id")
    private User player1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player2Id")
    private User player2;
    
    @Column(nullable = false)
    private String roomId;  // 추가
    
    @Column(nullable = false)
    private boolean isCompleted = true;  // 추가

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultType result;

    @Column(nullable = false)
    private LocalDateTime gameDate;
    
    @Column
    private String winner;  // 추가

    @Column
    private String loser;   // 추가

 
 // 생성자 수정
    public GameResult() {
        this.gameDate = LocalDateTime.now();
        this.isCompleted = true;
    }

    // 모든 필드를 포함한 생성자 (선택 사항)
    public GameResult(User player1, User player2, ResultType result, String roomId) {
        this.player1 = player1;
        this.player2 = player2;
        this.result = result;
        this.roomId = roomId;
        this.gameDate = LocalDateTime.now();
        this.isCompleted = true;
        
        
        //승자 패자 구분
        switch (result) {
        case WIN:
            this.winner = player1.getNickname();
            this.loser = player2.getNickname();
            break;
        case LOSE:
            this.winner = player2.getNickname();
            this.loser = player1.getNickname();
            break;
        case DRAW:
            this.winner = null;  // 무승부인 경우 둘 다 null
            this.loser = null;
            break;
    }
    }

    // Getter 및 Setter 메서드

    public Long getId() {
        return id;
    }

    public User getPlayer1() {
        return player1;
    }

    public void setPlayer1(User player1) {
        this.player1 = player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public void setPlayer2(User player2) {
        this.player2 = player2;
    }

    public ResultType getResult() {
        return result;
    }

    public void setResult(ResultType result) {
        this.result = result;
    }

    public LocalDateTime getGameDate() {
        return gameDate;
    }

    public void setGameDate(LocalDateTime gameDate) {
        this.gameDate = gameDate;
    }
    
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    // equals 및 hashCode 메서드 (닉네임을 기준으로)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GameResult that = (GameResult) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // toString 메서드 (필요 시 추가)
    @Override
    public String toString() {
        return "GameResult{" +
                "id=" + id +
                ", roomId='" + roomId + '\'' +
                ", player1=" + (player1 != null ? player1.getNickname() : "null") +
                ", player2=" + (player2 != null ? player2.getNickname() : "null") +
                ", result=" + result +
                ", gameDate=" + gameDate +
                ", isCompleted=" + isCompleted +
                '}';
    }
    
    public GameResultDTO toDTO() {
        GameResultDTO dto = new GameResultDTO();
        dto.setRoomId(this.roomId);
        dto.setWinnerNickname(this.winner);
        dto.setLoserNickname(this.loser);
        dto.setDraw(this.result == ResultType.DRAW);
        return dto;
    }
}
