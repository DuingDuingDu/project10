package com.hoit.checkers.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GameStateDTO {
    private String roomId;
    private Map<String, List<PiecePositionDTO>> pieces;
    private String currentTurn;
    private String player1Nickname;
    private String player2Nickname;
    private boolean gameStarted;

    // Getters and setters
}