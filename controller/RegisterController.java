package com.hoit.checkers.controller;

import com.hoit.checkers.dto.SignUpRequest;
import com.hoit.checkers.model.User;
import com.hoit.checkers.repository.UserRepository;
import com.hoit.checkers.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/register")
public class RegisterController {

    @Autowired
    private UserRepository repository;

    @Autowired
    private UserService userService; // UserService 주입

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    // 비밀번호 최소 길이와 특수 문자 포함 여부를 확인하기 위한 정규식
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{4,}$");
    
    private boolean isNicknameExists(String nickname, Model model) {
        Optional<User> optionalUser = repository.findByNickname(nickname);
        if (optionalUser.isPresent()) {
            logger.debug("중복된 닉네임 확인됨: {}", nickname);
            model.addAttribute("error", "이미 사용 중인 닉네임입니다.");
            return true;
        }
        return false;
    }

    @PostMapping
    public String register(@ModelAttribute SignUpRequest signUpRequest,
                           HttpSession session,
                           Model model) {
        String username = signUpRequest.getUsername();
        String password = signUpRequest.getPassword();
        String nickname = signUpRequest.getNickname();

        // 입력값 유효성 검사
        if (isInputInvalid(username, password, nickname, model)) {
            logger.debug("회원가입 실패: 입력값 유효성 검사 실패 - username: {}, nickname: {}", username, nickname);
            model.addAttribute("showRegisterModal", true);  // 회원가입 모달 표시 플래그
            return "main";
        }

        // 중복 사용자 이름 체크
        if (isUsernameExists(username, model)) {
            logger.debug("회원가입 실패: 중복된 사용자 이름 - username: {}", username);
            model.addAttribute("showRegisterModal", true);  // 회원가입 모달 표시 플래그
            return "main";
        }

        // 중복 닉네임 체크
        if (isNicknameExists(nickname, model)) {
            logger.debug("회원가입 실패: 중복된 닉네임 - nickname: {}", nickname);
            model.addAttribute("showRegisterModal", true);  // 회원가입 모달 표시 플래그
            return "main";
        }

        // 비밀번호를 해싱(암호화)하여 저장
        String hashedPassword = passwordEncoder.encode(password);

        // User 객체 생성 및 필드 설정
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashedPassword); // 해싱된 비밀번호 사용
        user.setNickname(nickname);
        user.setUserType(User.UserType.MEMBER); // 사용자 유형 설정 추가

        // 승/무/패 초기값 설정
        user.setWins(0);
        user.setDraws(0);
        user.setLosses(0);

        // DB에 사용자 정보 저장
        try {
            userService.save(user); // UserService를 통해 저장
            logger.info("회원가입 성공: username: {}", username);

            // 회원가입 성공 후 자동 로그인 처리
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 세션에 사용자 정보 저장 (필요 시)
            session.setAttribute("user", user);
            session.setAttribute("nickname", user.getNickname());

            // 로비로 리다이렉트
            return "redirect:/lobby";
        } catch (Exception e) {
            logger.error("회원가입 중 오류 발생 - username: {}, error: {}", username, e.getMessage(), e);
            model.addAttribute("error", "회원가입 중 오류가 발생했습니다. 다시 시도해 주세요.");
            model.addAttribute("showRegisterModal", true);  // 회원가입 모달 표시 플래그
            return "main";
        }
    }

    private boolean isInputInvalid(String username, String password, String nickname, Model model) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty() ||
                nickname == null || nickname.trim().isEmpty()) {
            logger.debug("입력값 유효성 검사 실패: 모든 필드가 필요합니다.");
            model.addAttribute("error", "모든 필드는 필수입니다.");
            return true;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            logger.debug("입력값 유효성 검사 실패: 비밀번호 형식 불일치 - password: {}", password);
            model.addAttribute("error", "비밀번호는 최소 4자 이상이어야 합니다.");
            return true;
        }
        return false;
    }

    private boolean isUsernameExists(String username, Model model) {
        Optional<User> optionalUser = repository.findByUsername(username);
        if (optionalUser.isPresent()) {
            logger.debug("중복된 사용자 이름 확인됨: {}", username);
            model.addAttribute("error", "이미 사용 중인 사용자 이름입니다.");
            return true;
        }
        return false;
    }
    
    
}
