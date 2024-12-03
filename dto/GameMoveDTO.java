package com.hoit.checkers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameMoveDTO {
    private String roomId;
    private String playerId;
    private int fromX;
    private int fromY;
    private int toX;
    private int toY;

    // Getters and setters
}