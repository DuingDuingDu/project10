package com.hoit.checkers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameOverResponse {
	
	private String winner;

    public GameOverResponse(String winner) {
        this.winner = winner;
    }

    public String getWinner() {
        return winner;
    }

}
