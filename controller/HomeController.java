package com.hoit.checkers.controller;

import com.hoit.checkers.model.User;
import com.hoit.checkers.service.UserService;
import com.hoit.checkers.service.LobbyService;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private LobbyService lobbyService;

    @GetMapping("/")
    public String home(Model model, HttpSession session, HttpServletRequest request) {
        // 기존 세션 무효화 (만약 기존 세션이 있다면)
        if (session != null) {
            String sessionId = session.getId();
            lobbyService.removeUser(sessionId);  // 로비 사용자 목록에서 제거
            session.invalidate();  // 기존 세션 무효화
        }

        // 새로운 세션 생성
        HttpSession newSession = request.getSession(true); // 새로운 세션 강제 생성

        // 모델 초기화
        model.addAttribute("error", "");
        model.addAttribute("showLoginModal", false);
        model.addAttribute("alertMessage", "");
        
        return "main";  // 메인 페이지로 이동
    }

    

    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        if (session != null) {
            String sessionId = session.getId();
            lobbyService.removeUser(sessionId);
            session.invalidate();  // 세션 무효화
        }

        // 쿠키 삭제 (Jakarta 기반)
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("JSESSIONID", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/"; // 홈 페이지로 리다이렉트
    }

    @GetMapping("/keepAlive")
    public ResponseEntity<Void> keepAlive(HttpSession session) {
        // 세션 타임아웃 방지를 위한 keep-alive 호출
        return ResponseEntity.ok().build();
    }
}
