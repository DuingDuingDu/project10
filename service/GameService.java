package com.hoit.checkers.service;

import com.hoit.checkers.exception.UserNotFoundException;
import com.hoit.checkers.model.GameResult;
import com.hoit.checkers.model.User;
import com.hoit.checkers.repository.GameResultRepository;
import com.hoit.checkers.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    @Autowired
    private GameResultRepository gameResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void processGameResult(String roomId, String winnerNickname, String loserNickname, boolean isDraw) {
        User winner = userRepository.findByNickname(winnerNickname)
                .orElseThrow(() -> new UserNotFoundException("Winner not found: " + winnerNickname));

        User loser = userRepository.findByNickname(loserNickname)
                .orElseThrow(() -> new UserNotFoundException("Loser not found: " + loserNickname));

        // 게임 결과 저장
        GameResult gameResult = new GameResult();
        gameResult.setPlayer1(winner);
        gameResult.setPlayer2(loser);

        if (isDraw) {
            gameResult.setResult(GameResult.ResultType.DRAW);
            winner.setDraws(winner.getDraws() + 1);
            loser.setDraws(loser.getDraws() + 1);
        } else {
            gameResult.setResult(GameResult.ResultType.WIN);
            winner.setWins(winner.getWins() + 1);
            loser.setLosses(loser.getLosses() + 1);
        }

        // 승률 재계산
        winner.calculateWinRate();
        loser.calculateWinRate();

        // 엔티티 저장
        gameResultRepository.save(gameResult);
        userRepository.save(winner);
        userRepository.save(loser);
    }
}
