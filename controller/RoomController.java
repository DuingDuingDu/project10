package com.hoit.checkers.controller;

import com.hoit.checkers.dto.*;
import com.hoit.checkers.exception.RoomFullException;
import com.hoit.checkers.model.*;
import com.hoit.checkers.repository.GameResultRepository;
import com.hoit.checkers.service.GameStateManager;
import com.hoit.checkers.service.RoomService;
import com.hoit.checkers.service.UserService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService; // UserService 주입

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private GameStateManager gameStateManager;
    
    @Autowired
    private GameResultRepository gameResultRepository;

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    /**
     * 방 생성 엔드포인트
     */
    @PostMapping("/create")
    public ResponseEntity<?> createRoom(@RequestBody RoomCreationRequest request, HttpSession session) {
        try {
            String nickname;

            // 현재 인증된 사용자 확인
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String
                     && authentication.getPrincipal().equals("anonymousUser"))) {
                // 인증된 사용자
                User user = userService.findByUsername(authentication.getName());
                nickname = user.getNickname();
            } else {
                // 비회원(게스트) 사용자
                User guest = userService.getCurrentUser(session);
                if (guest == null) {
                    // 게스트 정보가 없으면 새로운 닉네임 생성
                    String guestNickname = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
                    guest = userService.createGuestUser(session, guestNickname);
                }
                nickname = guest.getNickname();
            }

            // 방 생성 요청에 호스트 닉네임 추가
            request.setCreatedByNickname(nickname);

            // 방 생성 및 저장
            Room createdRoom = roomService.createRoom(request, nickname, session);

            // 사용자를 생성된 방에 자동으로 참여시키기 (이미 호스트가 추가됨)
            // 따라서 추가로 joinRoom을 호출할 필요 없음

            // 응답에 방의 ID와 메시지를 포함하여 JSON 반환
            Map<String, Object> response = new HashMap<>();
            response.put("id", createdRoom.getId());
            response.put("message", "방이 성공적으로 생성되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("방 생성 중 오류 발생", e);
            String errorMessage = "방 생성 중 에러가 발생했습니다: " + e.getMessage();
            String encodedError = UriUtils.encodeQueryParam(errorMessage, StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("방 생성 중 에러가 발생했습니다.");
        }
    }

    /**
     * 방 목록 가져오기 엔드포인트
     */
    @GetMapping("/list")
    public List<Map<String, Object>> getRoomList() {
        Collection<Room> rooms = roomService.getAllRooms();
        List<Map<String, Object>> roomList = new ArrayList<>();

        for (Room room : rooms) {
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("id", room.getId());
            roomData.put("name", room.getName());
            roomData.put("maxUsers", room.getMaxUsers());
            roomData.put("currentUsers", room.getCurrentUsers());
            roomData.put("privateRoom", room.isPrivateRoom());
            roomList.add(roomData);
        }

        return roomList;
    }

    /**
     * 방 참여 엔드포인트
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody RoomJoinRequest request, HttpSession session) {
        try {
            // 현재 사용자 가져오기
            User user = userService.getCurrentUser(session);
            if (user == null) {
                // 게스트 사용자 처리: 닉네임을 생성하여 게스트 사용자 생성
                String guestNickname = "Guest_" + UUID.randomUUID().toString().substring(0, 8);
                user = userService.createGuestUser(session, guestNickname);
            }

            // 방에 참여
            roomService.joinRoom(request.getRoomId(), user.getNickname(), user);
            
            // WebSocket을 통해 다른 사용자들에게 알림
            notifyRoomJoin(request.getRoomId(), user);

            return ResponseEntity.ok("방에 성공적으로 참여했습니다.");
        } catch (RoomFullException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            logger.error("방 참여 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("방 참여 중 에러가 발생했습니다: " + e.getMessage());
        }
    }

    private void notifyRoomJoin(String roomId, User user) {
        Room room = roomService.getRoomById(roomId);
        Room.UserStatus userStatus = room.getUserStatusMap().get(user.getNickname());
        
        OpponentInfo joinInfo = new OpponentInfo(
            user.getNickname(),
            user.getWins(),
            user.getDraws(),
            user.getLosses(),
            user.getWinRate(),
            userStatus.getRole()  // role 정보 추가
        );
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/opponent", joinInfo);
    }

    /**
     * 방 떠나기 엔드포인트
     */
    @PostMapping("/leave")
    public ResponseEntity<?> leaveRoom(@RequestBody RoomLeaveRequest request, HttpSession session) {
        logger.debug("Received leaveRoom request with roomId: {}", request.getRoomId());

        Room room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            logger.warn("Room not found with roomId: {}", request.getRoomId());
            return ResponseEntity.badRequest().body("방을 찾을 수 없습니다.");
        }

        // Attempt to retrieve the user from the session
        User user = (User) session.getAttribute("user");
        if (user == null) {
            user = (User) session.getAttribute("guestUser"); // 비회원 사용자도 체크
        }

        if (user == null) {
            logger.warn("User not found in session: {}", session.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("로그인이 필요합니다.");
        }

        if (!room.containsUserByNickname(user.getNickname())) {
            logger.warn("User {} is not part of room {}", user.getNickname(), room.getId());
            return ResponseEntity.badRequest().body("방에 속해 있지 않습니다.");
        }

        // 방에서 사용자 제거
        boolean left = roomService.leaveRoom(room, user);
        if (left) {
            logger.info("User {} left room {}", user.getNickname(), room.getId());
            
            // 세션에서 사용자 제거
            if (user.getUserType() == User.UserType.GUEST) {
                session.removeAttribute("guestUser");
            } else {
                session.removeAttribute("user");
            }

            return ResponseEntity.ok("방에서 나갔습니다.");
        } else {
            logger.warn("User {} failed to leave room {}", user.getNickname(), room.getId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("방에서 나갈 수 없습니다.");
        }
    }


    

    /**
     * WebSocket 메시지 핸들러: 준비 상태
     */
    @MessageMapping("/room.ready")
    @Transactional
    public void handleReadyStatus(@Payload ReadyStatusMessage message) {
        Room room = roomService.getRoomById(message.getRoomId());

        if (room == null) {
            return;
        }

        String username = message.getUsername();
        if (username == null || username.isEmpty()) {
            return;
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return;
        }

        boolean isReady = message.isReady();

        // Update the user's ready status
        room.setUserReadyStatusByNickname(user.getNickname(), isReady);
        roomService.saveRoom(room);  // 추가: 변경사항을 DB에 저장

        // Check if all players are ready
        boolean allReady = room.arePlayersReady();

        System.out.println("All players ready: " + allReady);  // 디버깅용

        // Create a response and send it
        ReadyStatusResponse response = new ReadyStatusResponse(allReady);
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId() + "/ready", response);
    }
    
    

    /**
     * WebSocket 메시지 핸들러: 게임 시작
     */
    @MessageMapping("/game.start")
    public void startGame(@Payload GameStartMessage message) {
        Room room = roomService.getRoomById(message.getRoomId());
        if (room != null && room.arePlayersReady()) {
            // player1과 player2 찾기
            User player1 = null;
            User player2 = null;
            
            for (Map.Entry<String, Room.UserStatus> entry : room.getUserStatusMap().entrySet()) {
                if (entry.getValue().getRole() == UserRole.PLAYER1) {
                    player1 = userService.findByNickname(entry.getKey());
                } else if (entry.getValue().getRole() == UserRole.PLAYER2) {
                    player2 = userService.findByNickname(entry.getKey());
                }
            }
            
            if (player1 != null && player2 != null) {
                // 게임 상태 생성
                gameStateManager.createGame(message.getRoomId(), player1, player2);
                
                GameStartResponse response = new GameStartResponse("게임이 시작되었습니다.");
                messagingTemplate.convertAndSend("/topic/game/" + message.getRoomId() + "/start", response);
                
                // 초기 게임 상태 전송
                messagingTemplate.convertAndSend("/topic/game/" + message.getRoomId() + "/state", 
                    gameStateManager.getGameState(message.getRoomId()));
            }
        }
    }
    

    @MessageMapping("/game.getState")
    public void handleGetGameState(@Payload GameStateRequest message) {
        Room room = roomService.getRoomById(message.getRoomId());
        if (room != null && room.getGameState() != null) {
            messagingTemplate.convertAndSend("/topic/game/" + message.getRoomId() + "/state", 
                room.getGameState());
        }
    }
    
    @MessageMapping("/game.move")
    public void handleMove(@Payload GameMoveDTO message) {  // GameMoveMessage 대신 GameMoveDTO 사용
        GameState state = gameStateManager.getGameState(message.getRoomId());
        if (state != null && state.getCurrentTurn().equals(message.getPlayerId())) {
            if (gameStateManager.isValidMove(message.getRoomId(), message.getPlayerId(),
                    message.getFromX(), message.getFromY(), message.getToX(), message.getToY())) {
                
                // 이동 처리 및 상태 업데이트
                state.movePiece(message.getFromX(), message.getFromY(), message.getToX(), message.getToY());
                gameStateManager.updateGameState(message.getRoomId(), state);

                // 상태 변경 브로드캐스트
                messagingTemplate.convertAndSend("/topic/game/" + message.getRoomId() + "/state", state);
                
                // 게임 종료 체크
                if (state.isGameOver()) {
                    handleGameOver(message.getRoomId(), state);
                }
            }
        }
    }
    
    @MessageMapping("/game.state")
    public void handleGameState(@Payload GameStateDTO message) {  // GameStateMessage 대신 GameStateDTO 사용
        Room room = roomService.getRoomById(message.getRoomId());
        if (room != null) {
            // GameStateDTO를 GameState로 변환하는 로직 필요
            GameState gameState = convertDTOToGameState(message);
            room.setGameState(gameState);
            roomService.saveRoom(room);
            
            messagingTemplate.convertAndSend("/topic/game/" + message.getRoomId() + "/state", message);
        }
    }
    
    private void handleGameOver(String roomId, GameState state) {
        // 게임 결과 DB 저장
    	GameResult result = new GameResult(
    	        state.getPlayer1(),
    	        state.getPlayer2(),
    	        state.determineResult(),
    	        roomId
    	    );
    	    
    	    gameResultRepository.save(result);
    	    gameStateManager.removeGame(roomId);

    	    // DTO로 변환하여 전송
    	    GameResultDTO resultDTO = result.toDTO();
    	    messagingTemplate.convertAndSend("/topic/game/" + roomId + "/gameOver", resultDTO);
    }


    /**
     * WebSocket 메시지 핸들러: 방 참여
     */
    @MessageMapping("/room.join")
    @SendTo("/topic/room/{roomId}/opponent")
    public RoomJoinResponse handleRoomJoin(@Payload RoomJoinRequest request) {
        Room room = roomService.getRoomById(request.getRoomId());
        if (room == null) {
            throw new IllegalArgumentException("방을 찾을 수 없습니다.");
        }

        User user = userService.findByNickname(request.getNickname());
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 방에 사용자 추가
        boolean isAdded = room.containsUserByNickname(user.getNickname());
        if (!isAdded) {
            boolean added = room.addUserByNickname(user.getNickname(), user);
            if (!added) {
                throw new IllegalStateException("방에 사용자 추가 실패");
            }
            roomService.saveRoom(room);
        }

        // 방의 현재 상태 정보를 모든 사용자에게 전송
        sendRoomStateToAll(room);

        return new RoomJoinResponse(user.getNickname(), user.getUserType().name(), room.getId());
    }
    
    private void sendRoomStateToAll(Room room) {
        Map<String, Room.UserStatus> userStatuses = room.getUserStatusMap();
        for (Map.Entry<String, Room.UserStatus> entry : userStatuses.entrySet()) {
            String nickname = entry.getKey();
            Room.UserStatus status = entry.getValue();
            User user = userService.findByNickname(nickname);
            
            OpponentInfo playerInfo = new OpponentInfo(
                user.getNickname(),
                user.getWins(),
                user.getDraws(),
                user.getLosses(),
                user.getWinRate(),
                status.getRole()
            );
            messagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/opponent", playerInfo);
        }
    }
    
    private GameState convertDTOToGameState(GameStateDTO dto) {
        Room room = roomService.getRoomById(dto.getRoomId());
        if (room == null) return null;

        User player1 = userService.findByNickname(dto.getPlayer1Nickname());
        User player2 = userService.findByNickname(dto.getPlayer2Nickname());
        
        GameState state = new GameState(dto.getRoomId(), player1, player2);
        // 필요한 상태 복사
        return state;
    }

    /**
     * WebSocket 메시지 핸들러: 방 삭제 알림
     * (선택 사항: 클라이언트에서 방 삭제를 처리할 경우 구현)
     */
    @MessageMapping("/room.delete")
    public void handleRoomDelete(@Payload RoomDeleteMessage message) {
        // 방 삭제 시 클라이언트에 알림을 보낼 수 있도록 구현
        messagingTemplate.convertAndSend("/topic/roomDeleted", message.getRoomId());
    }
    
    @MessageMapping("/room.requestPromotion")
    public void handlePromotionRequest(@Payload PromotionRequest request) {
        Room room = roomService.getRoomById(request.getRoomId());
        if (room != null) {
            try {
                User user = userService.findByNickname(request.getNickname());
                if (user != null) {
                    roomService.handlePromotionRequest(room, user);
                }
            } catch (Exception e) {
                logger.error("Error handling promotion request", e);
            }
        }
    }

    // 글로벌 예외 핸들러로 이동 권장
}
