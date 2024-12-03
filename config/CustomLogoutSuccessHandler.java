package com.hoit.checkers.config;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import com.hoit.checkers.service.LobbyService;

@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Autowired
    private LobbyService lobbyService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            // 로비에서 사용자 제거
            lobbyService.removeUser(username);
        }

        // 로그아웃 성공 후 홈으로 리다이렉트
        response.sendRedirect("/");
    }
}
