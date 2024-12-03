package com.hoit.checkers.controller;

import com.hoit.checkers.model.Room;
import com.hoit.checkers.model.User;
import com.hoit.checkers.service.RoomService;
import com.hoit.checkers.service.UserService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    private final RoomService roomService;
    private final UserService userService;

    public PageController(RoomService roomService, UserService userService) {
        this.roomService = roomService;
        this.userService = userService;
    }

    @GetMapping("/rooms/{roomId}")
    public String getRoom(@PathVariable String roomId, Model model, HttpSession session) {
        // 방 정보 로딩
        Room room = Optional.ofNullable(roomService.getRoomById(roomId))
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // 사용자 정보 로딩
        User user = getOrCreateUser(session);
        
        // appData 구성
        Map<String, Object> appData = createAppData(room, user);
        model.addAttribute("appData", appData);

        return "room";
    }

    private User getOrCreateUser(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // 인증된 사용자 처리
        if (isAuthenticatedUser(auth)) {
            return Optional.ofNullable(userService.findByUsername(auth.getName()))
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        }
        
        // 세션에서 게스트 사용자 확인
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            return sessionUser;
        }

        // 새 게스트 사용자 생성
        String guestNickname = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
        return userService.createGuestUser(session, guestNickname);
    }

    private Map<String, Object> createAppData(Room room, User user) {
        Map<String, Object> appData = new HashMap<>();
        
        // 사용자 데이터 구성
        appData.put("roomId", room.getId());
        appData.put("user", createUserData(user));
        appData.put("userRole", getUserRole(room, user));
        appData.put("isHost", isUserHost(room, user));
        appData.put("roomState", createRoomState(room));
        
        return appData;
    }

    private Map<String, Object> createUserData(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("nickname", user.getNickname());
        userData.put("userType", user.getUserType());
        userData.put("wins", user.getWins());
        userData.put("draws", user.getDraws());
        userData.put("losses", user.getLosses());
        userData.put("winRate", user.getWinRate());
        return userData;
    }

    private Map<String, Object> createRoomState(Room room) {
        Map<String, Object> roomState = new HashMap<>();
        Map<String, Object> players = new HashMap<>();

        room.getUserStatusMap().forEach((nickname, status) -> {
            User player = userService.findByNickname(nickname);
            players.put(nickname, createPlayerInfo(player, status));
        });

        roomState.put("players", players);
        return roomState;
    }

    private Map<String, Object> createPlayerInfo(User player, Room.UserStatus status) {
        Map<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("nickname", player.getNickname());
        playerInfo.put("wins", player.getWins());
        playerInfo.put("draws", player.getDraws());
        playerInfo.put("losses", player.getLosses());
        playerInfo.put("winRate", player.getWinRate());
        playerInfo.put("role", status.getRole());
        return playerInfo;
    }

    private String getUserRole(Room room, User user) {
        return Optional.ofNullable(room.getUserStatusMap().get(user.getNickname()))
                .map(status -> status.getRole().toString())
                .orElse("OBSERVER");
    }

    private boolean isUserHost(Room room, User user) {
        return Optional.ofNullable(room.getHost())
                .map(host -> host.getNickname().equals(user.getNickname()))
                .orElse(false);
    }

    private boolean isAuthenticatedUser(Authentication auth) {
        return auth != null && 
               auth.isAuthenticated() && 
               !(auth.getPrincipal() instanceof String && 
                 "anonymousUser".equals(auth.getPrincipal()));
    }
}