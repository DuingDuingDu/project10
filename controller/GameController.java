package com.hoit.checkers.controller;

import com.hoit.checkers.dto.GameResultDTO;
import com.hoit.checkers.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/result")
    public ResponseEntity<String> submitGameResult(@RequestBody GameResultDTO gameResultDTO) {
        try {
            gameService.processGameResult(
                    gameResultDTO.getRoomId(),
                    gameResultDTO.getWinnerNickname(),
                    gameResultDTO.getLoserNickname(),
                    gameResultDTO.isDraw()
            );
            return ResponseEntity.ok("Game result processed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing game result: " + e.getMessage());
        }
    }
}
