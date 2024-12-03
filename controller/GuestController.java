package com.hoit.checkers.controller;

import com.hoit.checkers.dto.GuestLoginRequest;
import com.hoit.checkers.model.User;
import com.hoit.checkers.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/guest")
public class GuestController {

    // 닉네임 중복 체크를 위한 맵 (애플리케이션 범위)
    private Set<String> guestNicknames = ConcurrentHashMap.newKeySet();
    
    private static final Logger logger = LoggerFactory.getLogger(GuestController.class);

    @Autowired
    private UserService userService; // UserService 주입

    @PostMapping("/login")
    public String guestLogin(@ModelAttribute GuestLoginRequest guestLoginRequest,
                             HttpSession session,
                             Model model) {
        String nickname = guestLoginRequest.getNickname();

        if (nickname == null || nickname.trim().isEmpty()) {
            logger.debug("게스트 로그인 실패: 닉네임이 비어 있음");
            model.addAttribute("error", "닉네임은 필수입니다.");
            return "main";
        }

        // 닉네임 중복 체크 via guestNicknames set
        if (guestNicknames.contains(nickname)) {
            model.addAttribute("error", "해당 닉네임은 이미 사용 중입니다.");
            return "main";
        }

        // 닉네임 등록
        guestNicknames.add(nickname);

        // 새 게스트 사용자 생성 및 세션에 저장
        try {
            userService.createGuestUser(session, nickname);
        } catch (Exception e) {
            logger.error("게스트 사용자 생성 중 오류 발생: {}", e.getMessage());
            model.addAttribute("error", "게스트 로그인 중 오류가 발생했습니다.");
            guestNicknames.remove(nickname); // 닉네임 제거
            return "main";
        }

        // 로비로 리다이렉트
        return "redirect:/lobby";
    }

    // 로그아웃 또는 세션 종료 시 닉네임 제거
    @EventListener
    public void handleSessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        User user = (User) session.getAttribute("user");
        if (user != null && user.getUserType() == User.UserType.GUEST) {
            guestNicknames.remove(user.getNickname());
        }
    }
}
