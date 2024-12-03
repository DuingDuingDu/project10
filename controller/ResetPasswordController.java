package com.hoit.checkers.controller;

import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import com.hoit.checkers.model.User;
import com.hoit.checkers.repository.UserRepository;

@RestController
@RequestMapping("/reset-password")
public class ResetPasswordController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestParam String username, @RequestParam String nickname) {
        Optional<User> optionalUser = userRepository.findByUsernameAndNickname(username, nickname);
        if (optionalUser.isPresent()) {
            // 사용자 정보를 확인 후, 새 비밀번호를 설정할 수 있는 페이지로 이동할 수 있는 토큰 발급 등 추가 로직 필요
            return ResponseEntity.ok("User verified. Proceed to reset password.");
        } else {
            // 사용자 정보를 찾을 수 없는 경우
            return ResponseEntity.status(400).body("입력하신 정보가 맞지 않습니다.");
        }
    }

    @PostMapping
    public ResponseEntity<String> resetPassword(@RequestParam String username,
                                                @RequestParam String newPassword,
                                                @RequestParam String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.status(400).body("비밀번호가 일치하지 않습니다.");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPassword(hashedPassword);
            userRepository.save(user);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } else {
            return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
        }
    }
}
