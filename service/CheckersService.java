package com.hoit.checkers.service;

import org.springframework.stereotype.Service;

@Service
public class CheckersService {
    private String[][] board; // 8x8 체스판

    public CheckersService() {
        // 초기 게임 보드 설정
        board = new String[8][8];
        initializeBoard();
    }

    private void initializeBoard() {
        // 체커 초기 상태 설정 (흑/백 돌 배치)
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (i < 3 && (i + j) % 2 == 1) {
                    board[i][j] = "B"; // 흑돌
                } else if (i > 4 && (i + j) % 2 == 1) {
                    board[i][j] = "W"; // 백돌
                } else {
                    board[i][j] = "";
                }
            }
        }
    }

    public String getGameState() {
        // 게임 상태를 JSON 형식으로 반환 (여기서는 간단하게 문자열로 표현)
        return "{\"board\": \"" + boardToString() + "\"}";
    }

    public boolean makeMove(String move) {
        // move를 받아서 유효한지 확인하고, 이동 처리
        // 예시: move = "2,3 to 4,5"
        String[] parts = move.split(" to ");
        if (parts.length != 2) {
            return false;
        }

        String[] from = parts[0].split(",");
        String[] to = parts[1].split(",");

        if (from.length != 2 || to.length != 2) {
            return false;
        }

        try {
            int fromRow = Integer.parseInt(from[0]);
            int fromCol = Integer.parseInt(from[1]);
            int toRow = Integer.parseInt(to[0]);
            int toCol = Integer.parseInt(to[1]);

            // 범위 검사
            if (isOutOfBounds(fromRow, fromCol) || isOutOfBounds(toRow, toCol)) {
                return false;
            }

            // 이동 가능 여부 검사 및 이동 처리 (간단한 예시)
            if (board[fromRow][fromCol] != null && board[toRow][toCol].isEmpty()) {
                board[toRow][toCol] = board[fromRow][fromCol];
                board[fromRow][fromCol] = "";
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }

    private boolean isOutOfBounds(int row, int col) {
        return row < 0 || row >= 8 || col < 0 || col >= 8;
    }

    private String boardToString() {
        // 보드를 문자열로 변환 (프론트엔드로 보낼 때 JSON 형태로 가공)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                sb.append(board[i][j].isEmpty() ? "." : board[i][j]);
                if (j < 7) sb.append(",");
            }
            if (i < 7) sb.append(";\n");
        }
        return sb.toString();
    }
}