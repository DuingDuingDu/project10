package com.hoit.checkers.controller;

import com.hoit.checkers.service.CheckersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
public class CheckersGameController {

    @Autowired
    private CheckersService checkersService; // 게임 로직 처리

    @GetMapping
    public ResponseEntity<String> getGameState() {
        // 게임 상태를 JSON으로 반환
        String gameState = checkersService.getGameState();
        return new ResponseEntity<>(gameState, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Void> makeMove(@RequestParam("move") String move) {
        // 클라이언트가 보낸 이동 정보를 받아서 처리
        boolean success = checkersService.makeMove(move);

        if (success) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}