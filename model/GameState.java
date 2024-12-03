package com.hoit.checkers.model;

import java.util.*;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GameState {
    private String roomId;
    private Map<String, List<PiecePosition>> playerPieces;
    private String currentTurn;
    private boolean gameStarted;
    private User player1;
    private User player2;
    
    public GameState(String roomId, User player1, User player2) {
        this.roomId = roomId;
        this.player1 = player1;
        this.player2 = player2;
        this.playerPieces = new HashMap<>();
        this.currentTurn = "PLAYER1";
        this.gameStarted = false;
        initializeBoard();
    }
    
    public User getPlayer1() {
        return player1;
    }

    public User getPlayer2() {
        return player2;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    private void initializeBoard() {
        List<PiecePosition> player1Pieces = new ArrayList<>();
        List<PiecePosition> player2Pieces = new ArrayList<>();

        // brown 칸에만 기물 배치
        for (int row = 0; row < 4; row++) {
            for (int col = (row % 2 == 0 ? 1 : 0); col < 10; col += 2) {
                player1Pieces.add(new PiecePosition(col, row));
                player2Pieces.add(new PiecePosition(col, 9 - row));
            }
        }

        playerPieces.put("PLAYER1", player1Pieces);
        playerPieces.put("PLAYER2", player2Pieces);
    }

    public boolean isGameOver() {
        return playerPieces.get("PLAYER1").isEmpty() || 
               playerPieces.get("PLAYER2").isEmpty();
    }

    public GameResult.ResultType determineResult() {
        if (playerPieces.get("PLAYER1").isEmpty()) {
            return GameResult.ResultType.LOSE;
        } else if (playerPieces.get("PLAYER2").isEmpty()) {
            return GameResult.ResultType.WIN;
        }
        return GameResult.ResultType.DRAW;
    }
    
    public void movePiece(int fromX, int fromY, int toX, int toY) {
        List<PiecePosition> pieces = playerPieces.get(currentTurn);
        
        // 기존 위치의 기물 제거
        pieces.removeIf(p -> p.getX() == fromX && p.getY() == fromY);
        
        // 새 위치에 기물 추가
        pieces.add(new PiecePosition(toX, toY));
        
        // 턴 변경
        currentTurn = currentTurn.equals("PLAYER1") ? "PLAYER2" : "PLAYER1";
    }
    
    public String getWinner() {
        if (playerPieces.get("PLAYER1").isEmpty()) {
            return player2.getNickname();
        } else if (playerPieces.get("PLAYER2").isEmpty()) {
            return player1.getNickname();
        }
        return null;
    }
    
    

    // Getters and setters
}