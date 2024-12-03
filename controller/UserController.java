package com.hoit.checkers.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.hoit.checkers.model.User;
import com.hoit.checkers.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/nickname")
    @ResponseBody
    public ResponseEntity<String> getNickname(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
                String username = authentication.getName();
                user = userService.findByUsername(username);
            }
        }
        if (user != null) {
            return ResponseEntity.ok(user.getNickname());
        } else {
            return ResponseEntity.status(401).body("No user logged in");
        }
    }

    // 추가적인 사용자 관련 API 엔드포인트를 여기서 관리
    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<?> getUserInfo(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
                String username = authentication.getName();
                user = userService.findByUsername(username);
            }
        }
        if (user != null) {
            // 필요한 사용자 정보를 모두 포함한 User 객체 또는 DTO 생성 후 반환
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "No user logged in"));
        }
    }
}
