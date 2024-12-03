package com.hoit.checkers.listener;

import com.hoit.checkers.service.LobbyService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    @Autowired
    private LobbyService lobbyService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        HttpSession session = (HttpSession) headers.getSessionAttributes().get("session");

        if (session != null) {
            String sessionId = session.getId();

            // 세션 유효성 체크
            if (session.isNew()) {
                // 만약 세션이 새로운 세션이라면 제거하지 않고 로그를 남김
                System.out.println("New session detected, skipping removal for session: " + sessionId);
                return;
            }

            // 세션이 유효하고 기존 사용자일 경우에만 제거 처리
            lobbyService.removeUser(sessionId);
            System.out.println("User disconnected: " + sessionId);
        }
    }
}
