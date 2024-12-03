package com.hoit.checkers.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.hoit.checkers.model.LobbyUser;
import com.hoit.checkers.service.LobbyService;

import jakarta.servlet.http.HttpSession;

import java.util.Collection;
import java.util.Map;

@Controller
public class LobbyWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LobbyService lobbyService;

    /**
     * 사용자가 로비에 접속할 때 호출되는 메서드입니다.
     * @param user 접속한 사용자의 정보
     */
    @MessageMapping("/joinLobby")
    public void joinLobby(@Payload LobbyUser user) {
        // LobbyService를 통해 사용자 추가
        lobbyService.addUser(user);

        // 업데이트된 사용자 목록을 모든 클라이언트에게 브로드캐스트
        Collection<LobbyUser> users = lobbyService.getAllUsers();
        messagingTemplate.convertAndSend("/topic/lobbyUsers", users);
    }

    /**
     * 사용자가 로비를 떠날 때 호출되는 메서드입니다.
     * @param uniqueId 사용자의 고유 식별자 (예: 세션 ID)
     */
    @MessageMapping("/leaveLobby")
    public void handleLeaveLobby(@Payload Map<String, String> payload) {
        String uniqueId = payload.get("sessionId");
        String nickname = payload.get("nickname");

        if (uniqueId != null) {
            lobbyService.removeUser(uniqueId);
        }
    }
    
    @PostMapping("/api/lobby/leave")
    public ResponseEntity<Void> leaveLobby(@RequestBody Map<String, String> payload, HttpSession session) {
        String uniqueId = payload.get("sessionId");
        String nickname = payload.get("nickname");

        if (uniqueId != null) {
            lobbyService.removeUser(uniqueId);
        }

        // 세션은 무효화하지 않음
        // session.invalidate(); // 이 코드가 있으면 제거합니다.

        return ResponseEntity.ok().build();
    }
}