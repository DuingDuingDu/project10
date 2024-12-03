package com.hoit.checkers.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PiecePosition {
    private int x;
    private int y;
    private boolean isKing;

    public PiecePosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.isKing = false;
    }

    // Getters and setters
}