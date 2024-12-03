package com.hoit.checkers.service;

import com.hoit.checkers.model.*;

import com.hoit.checkers.repository.GameResultRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class GameStateManager {
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final GameResultRepository gameResultRepository;

    public GameStateManager(GameResultRepository gameResultRepository) {
        this.gameResultRepository = gameResultRepository;
    }

    public void createGame(String roomId, User player1, User player2) {
        activeGames.put(roomId, new GameState(roomId, player1, player2));
    }

    public GameState getGameState(String roomId) {
        return activeGames.get(roomId);
    }

    public void updateGameState(String roomId, GameState state) {
        activeGames.put(roomId, state);
    }

    public void finishGame(String roomId) {
        GameState state = activeGames.get(roomId);
        if (state != null && state.isGameOver()) {
            GameResult gameResult = new GameResult(
                state.getPlayer1(),
                state.getPlayer2(),
                state.determineResult(),
                roomId  // 새로운 생성자에 맞게 roomId 추가
            );
            gameResultRepository.save(gameResult);
            activeGames.remove(roomId);
        }
    }

    public boolean isValidMove(String roomId, String playerId, int fromX, int fromY, int toX, int toY) {
        GameState state = getGameState(roomId);
        if (state == null || !state.getCurrentTurn().equals(playerId)) {
            return false;
        }
        // 체커스 게임 규칙에 따른 이동 유효성 검사 로직 구현
        return true; // 임시 반환
    }
    
    public void removeGame(String roomId) {
        activeGames.remove(roomId);
    }
}