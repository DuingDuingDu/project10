package com.hoit.checkers.listener;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.hoit.checkers.service.LobbyService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserSessionListener implements HttpSessionListener {

    @Autowired
    private LobbyService lobbyService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 접속 중인 사용자 목록 (세션 ID를 키로 사용)
    private static final Map<String, String> activeUsers = new ConcurrentHashMap<>();

    public static Map<String, String> getActiveUsers() {
        return activeUsers;
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        // 세션이 생성될 때 필요에 따라 사용자를 관리할 수 있음.
        // 사용자의 닉네임 등을 초기화하거나 특정 값을 할당하는 등의 작업 가능
        String sessionId = se.getSession().getId();
        System.out.println("Session Created: " + sessionId);
        // 필요한 경우 사용자 닉네임이나 상태를 추가할 수 있습니다.
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // 세션 소멸 시 호출
        String sessionId = se.getSession().getId();

        // 로비 서비스에서 사용자 제거
        lobbyService.removeUser(sessionId);
       

        // 접속 중인 사용자 목록에서 제거
        activeUsers.remove(sessionId);
        messagingTemplate.convertAndSend("/topic/lobbyUsers", lobbyService.getAllUsers()); // 로비 목록 갱신

        System.out.println("Session Destroyed: " + sessionId);
    }
}
