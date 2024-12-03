package com.hoit.checkers.controller;

import com.hoit.checkers.model.ChatMessage;
import com.hoit.checkers.model.LobbyUser;
import com.hoit.checkers.model.User;
import com.hoit.checkers.service.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.context.event.EventListener;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LobbyService lobbyService; // LobbyService 주입

    @MessageMapping("/user.join")
    public void join(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        LobbyUser user = lobbyService.getUser(sessionId);

        if (user != null) {
            // 사용자 목록 업데이트를 모든 클라이언트에게 전송
            messagingTemplate.convertAndSend("/topic/userList", lobbyService.getAllUsers());
            System.out.println("User joined: " + user.getNickname());
        }
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        System.out.println("Received chat message from " + chatMessage.getSender() + ": " + chatMessage.getContent());

        // 메시지가 특정 방에 속해 있는지 확인
        String roomId = chatMessage.getRoomId();
        if (roomId != null && !roomId.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/chat/" + roomId, chatMessage);
        } else {
            // 기본 채팅 주제로 전송 (로비 등)
            messagingTemplate.convertAndSend("/topic/chat", chatMessage);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        lobbyService.removeUser(sessionId);
        System.out.println("User disconnected: " + sessionId);

        // 업데이트된 사용자 목록을 모든 클라이언트에게 전송
        messagingTemplate.convertAndSend("/topic/userList", lobbyService.getAllUsers());
    }
}
