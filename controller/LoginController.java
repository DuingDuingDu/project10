package com.hoit.checkers.controller;

import com.hoit.checkers.listener.UserSessionListener;
import com.hoit.checkers.model.LobbyUser;
import com.hoit.checkers.model.User;
import com.hoit.checkers.repository.CheckersRepository;
import com.hoit.checkers.service.LobbyService;
import com.hoit.checkers.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private LobbyService lobbyService;

    // 로그인 페이지에 접근하는 GET 요청 처리
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model, HttpServletResponse response) {
        // 캐시 무효화 설정
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // 로그인 오류 처리
        if (error != null) {
            model.addAttribute("error", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
            model.addAttribute("showLoginModal", true);
        } else {
            model.addAttribute("error", "");
            model.addAttribute("showLoginModal", false);
        }
        model.addAttribute("alertMessage", "");
        return "main";
    }


    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = userService.authenticate(username, password);

            if (user == null) {
                model.addAttribute("error", "Invalid username or password");
                model.addAttribute("showLoginModal", true);
                return "main";
            }

            session.setAttribute("user", user);
            session.setAttribute("nickname", user.getNickname());

            String sessionId = session.getId();

            // 로비에 이미 해당 세션이 존재하는지 확인
            if (lobbyService.isUserInLobby(sessionId)) {
                return "redirect:/lobby";
            }

            // 닉네임 중복 체크
            if (user.isGuest()) {
                if (lobbyService.isNicknameTaken(user.getNickname())) {
                    model.addAttribute("error", "Guest nickname is already taken.");
                    model.addAttribute("showLoginModal", true);
                    return "main";
                }
                // 게스트 사용자 로비에 추가
                lobbyService.addGuestUser(sessionId, user);
            } else {
                if (lobbyService.isNicknameTaken(user.getNickname())) {
                    model.addAttribute("error", "Nickname is already taken.");
                    model.addAttribute("showLoginModal", true);
                    return "main";
                }
                // 회원 사용자 로비에 추가
                lobbyService.addUser(sessionId, user);
            }

            // 닉네임을 모델에 추가
            model.addAttribute("nickname", user.getNickname());
            return "redirect:/lobby";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "로그인 중 에러가 발생했습니다.");
            model.addAttribute("showLoginModal", true);
            return "main";
        }
    }
    
    @GetMapping("/login?error")
    public String loginError(Model model) {
        model.addAttribute("error", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
        model.addAttribute("showLoginModal", true);
        return "main";
    }

    
    // 비밀번호 찾기 요청 처리
    @PostMapping("/recover-password")
    public String recoverPassword(@RequestParam("username") String username,
                                  @RequestParam("nickname") String nickname,
                                  @RequestParam("newPassword") String newPassword,
                                  Model model) {
        // 데이터베이스에서 사용자명과 닉네임을 확인합니다.
        Optional<User> userOptional = userService.findByUsernameAndNickname(username, nickname);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 새로운 비밀번호를 암호화하여 설정합니다.
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            userService.save(user);
            model.addAttribute("alertMessage", "비밀번호가 수정되었습니다.");
            return "main"; // 성공 시 메인 페이지로 이동하며 알림 메시지 표시
        } else {
            // 일치하는 사용자를 찾지 못한 경우
            model.addAttribute("alertMessage", "Username 혹은 Nickname이 올바르지 않습니다.");
            model.addAttribute("showRecoveryModal", true);
            return "main"; // 비밀번호 찾기 실패 시 메인 페이지로 이동하면서 에러 정보를 전달
        }
    }
}


