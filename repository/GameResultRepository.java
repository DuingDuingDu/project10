package com.hoit.checkers.repository;

import com.hoit.checkers.model.GameResult;
import com.hoit.checkers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    // 특정 사용자가 참여한 모든 게임 결과 조회
    List<GameResult> findByPlayer1OrPlayer2(User player1, User player2);

    // 특정 사용자와의 게임 결과 조회
    List<GameResult> findByPlayer1AndPlayer2(User player1, User player2);

    // 최근 10개의 게임 결과 조회 (최신 순)
    List<GameResult> findByPlayer1OrPlayer2OrderByGameDateDesc(User player1, User player2);
    List<GameResult> findTop10ByOrderByGameDateDesc();

    // 결과 타입별 게임 결과 조회
    List<GameResult> findByResult(GameResult.ResultType resultType);

    // 특정 기간 내의 게임 결과 조회
    List<GameResult> findByGameDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<GameResult> findByRoomId(String roomId);
    List<GameResult> findByPlayer1NicknameOrPlayer2Nickname(String player1Nickname, String player2Nickname);
    
    
}
