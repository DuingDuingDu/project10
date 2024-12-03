package com.hoit.checkers.controller;

import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hoit.checkers.model.LobbyUser;
import com.hoit.checkers.model.User;
import com.hoit.checkers.service.LobbyService;
import com.hoit.checkers.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class LobbyController {

    @Autowired
    private UserService userService;

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(LobbyController.class);

    @GetMapping("/lobby")
    public String lobby(HttpSession session, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user;
        boolean isGuest = false;
        String uniqueId;

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            // 인증된 사용자가 아닌 경우: 게스트 사용자 처리
            user = (User) session.getAttribute("user");
            if (user == null || user.getUserType() != User.UserType.GUEST) {
                // 세션에 사용자 정보가 없거나 기존 세션에 잘못된 정보가 있을 경우 새로 생성
                String guestNickname = (String) session.getAttribute("nickname");
                if (guestNickname == null) {
                    guestNickname = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
                }
                user = new User();
                user.setNickname(guestNickname);
                user.setUserType(User.UserType.GUEST);
                session.setAttribute("user", user);
            }
            uniqueId = session.getId(); // 게스트는 sessionId를 uniqueId로 사용
            user.setGuestId(uniqueId); // User 엔티티의 guestId에 sessionId 설정
            isGuest = true;
        } else {
            // 인증된 사용자 처리
            String username = authentication.getName();
            user = userService.findByUsername(username);

            if (user == null) {
                throw new IllegalStateException("인증된 사용자의 정보를 찾을 수 없습니다.");
            }

            // 회원 정보가 올바르게 세션에 저장되었는지 확인 후 저장
            if (session.getAttribute("user") == null || user.getUserType() == User.UserType.GUEST) {
                session.setAttribute("user", user);
            }

            uniqueId = user.getUniqueId(); // 회원은 uniqueId를 사용
            user.setGuestId(null); // 회원의 guestId는 null로 설정
            isGuest = false;
        }

        // 사용자 정보를 모델에 추가하는 메서드 호출
        addUserToModel(session, model, user, isGuest, uniqueId);

        // 로그인 여부와 게스트 여부를 모델에 추가
        model.addAttribute("isLoggedIn", !isGuest);
        model.addAttribute("isGuest", isGuest);

        // 로비 사용자 목록 추가
        model.addAttribute("lobbyUsers", lobbyService.getAllUsers());

        logger.debug("User: {}, isLoggedIn: {}, isGuest: {}", user.getNickname(), !isGuest, isGuest);

        return "lobby";
    }

    private void addUserToModel(HttpSession session, Model model, User user, boolean isGuest, String uniqueId) {
        // 닉네임 중복 체크 (현재 사용자 제외)
        boolean nicknameTaken = lobbyService.isNicknameTaken(user.getNickname(), uniqueId);

        if (nicknameTaken) {
            // 닉네임 중복 시 새로운 닉네임 생성
            String newNickname = user.getNickname() + "_" + UUID.randomUUID().toString().substring(0, 4);
            user.setNickname(newNickname);
            session.setAttribute("nickname", newNickname); // 세션에 새로운 닉네임 저장
        }

        // 사용자 추가 또는 업데이트
        if (isGuest) {
            lobbyService.addGuestUser(uniqueId, user);
        } else {
            lobbyService.addUser(uniqueId, user);
        }

        // 모델에 사용자 정보 추가
        model.addAttribute("nickname", user.getNickname());
        model.addAttribute("wins", user.getWins());
        model.addAttribute("draws", user.getDraws());
        model.addAttribute("losses", user.getLosses());
        model.addAttribute("winRate", String.format("%.2f", user.getWinRate()));
        model.addAttribute("isLoggedIn", !user.isGuest());
        model.addAttribute("isGuest", user.isGuest());

        // 모든 사용자를 브로드캐스트하여 클라이언트가 실시간으로 업데이트
        Collection<LobbyUser> users = lobbyService.getAllUsers();
        messagingTemplate.convertAndSend("/topic/lobbyUsers", users);
    }

    public void removeUserFromLobby(String sessionId, String uniqueId) {
        if (uniqueId == null && sessionId == null) {
            // 둘 다 null인 경우 로그 출력 후 종료
            logger.warn("Both uniqueId and sessionId are null. Cannot remove user from lobby.");
            return;
        }

        // 로비에서 사용자 제거
        lobbyService.removeUser(uniqueId != null ? uniqueId : sessionId);

        // SimpMessagingTemplate을 사용해 로비 사용자 목록 갱신
        messagingTemplate.convertAndSend("/topic/lobbyUsers", lobbyService.getAllUsers());
    }

    @GetMapping("/leaveLobby")
    public String leaveLobby(HttpSession session) {
        if (session != null) {
            String sessionId = session.getId();
            User user = (User) session.getAttribute("user");
            String uniqueId = null;

            if (user != null) {
                uniqueId = user.getUniqueId();
                if (uniqueId == null) {
                    // 게스트 사용자의 경우 guestId 사용
                    uniqueId = user.getGuestId();
                }
            }

            // 로비에서 사용자 제거
            removeUserFromLobby(sessionId, uniqueId);

            // 세션은 무효화하지 않음
            // 세션을 유지하여 게임 기록 등을 관리할 수 있도록 함
        }
        return "redirect:/"; // 홈 페이지로 리다이렉트
    }
}
