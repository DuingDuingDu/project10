package com.hoit.checkers.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hoit.checkers.dto.RoomCreationRequest;
import com.hoit.checkers.exception.RoomFullException;
import com.hoit.checkers.model.Room;
import com.hoit.checkers.model.User;
import com.hoit.checkers.model.UserRole;
import com.hoit.checkers.repository.RoomRepository;
import com.hoit.checkers.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class RoomService {

	@Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);
    
    private static final int FIXED_PLAYERS = 2;  // 항상 2명의 플레이어

    /**
     * 방 생성 메서드
     */
    @Transactional
    public Room createRoom(RoomCreationRequest request, String createdByNickname, HttpSession session) throws Exception {
        Room room = new Room();
        room.setName(request.getName());
        // 요청된 최대 인원에서 2명은 플레이어, 나머지는 observer로 설정
        int maxObservers = Math.max(0, request.getMaxUsers() - FIXED_PLAYERS);
        room.setMaxUsers(request.getMaxUsers());
        room.setMaxObservers(maxObservers);
        room.setCreatedByNickname(createdByNickname);

        if (request.isPrivateRoom()) {
            room.setPassword(request.getPassword());
        }

        User hostUser;
        try {
            hostUser = userService.findByNickname(createdByNickname);
        } catch (UsernameNotFoundException e) {
            hostUser = createOrGetGuestUser(createdByNickname);
        }

        room.setHost(hostUser);
        
        // 호스트를 Player1으로 설정
        Room.UserStatus hostStatus = new Room.UserStatus();
        hostStatus.setRole(UserRole.PLAYER1);
        hostStatus.setReady(false);
        room.getUserStatusMap().put(createdByNickname, hostStatus);

        roomRepository.save(room);
        logger.debug("Room created with host {} as PLAYER1, max users: {}, max observers: {}", 
            hostUser.getNickname(), request.getMaxUsers(), maxObservers);

        // 방 생성 알림 전송
        notifyRoomCreation(room);
        
        return room;
    }
    
    private void notifyRoomCreation(Room room) {
        Map<String, Object> roomInfo = new HashMap<>();
        roomInfo.put("roomId", room.getId());
        roomInfo.put("name", room.getName());
        roomInfo.put("maxUsers", room.getMaxUsers());
        roomInfo.put("maxObservers", room.getMaxObservers());
        roomInfo.put("currentUsers", room.getCurrentUsers());
        messagingTemplate.convertAndSend("/topic/roomCreated", roomInfo);
    }

    /**
     * 게스트 사용자 생성 또는 기존 게스트 사용자 가져오기
     */
    @Transactional
    private User createOrGetGuestUser(String createdByNickname) throws Exception {
        Optional<User> optionalUser = userRepository.findByNickname(createdByNickname);
        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();
            if (existingUser.getUserType() == User.UserType.GUEST) {
                return existingUser;
            } else {
                throw new Exception("닉네임이 이미 사용 중입니다.");
            }
        }

        User guestUser = new User();
        guestUser.setUsername("guest_" + UUID.randomUUID().toString().substring(0, 8));
        guestUser.setNickname(createdByNickname);
        guestUser.setPassword("guest_password");
        guestUser.setUserType(User.UserType.GUEST);
        guestUser.setWins(0);
        guestUser.setDraws(0);
        guestUser.setLosses(0);
        guestUser.setWinRate(0.0);

        userRepository.save(guestUser);
        logger.debug("Guest user created with username: {}", guestUser.getUsername());

        return guestUser;
    }

    /**
     * 방 참여 메서드
     */
    @Transactional
    public void joinRoom(String roomId, String nickname, User user) throws Exception {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new Exception("방을 찾을 수 없습니다."));

        if (room.getUserStatusMap().containsKey(nickname)) {
            throw new Exception("이미 방에 참여 중입니다.");
        }

        // 현재 플레이어 수와 관전자 수 계산
        long playerCount = room.getUserStatusMap().values().stream()
                .filter(status -> status.getRole() == UserRole.PLAYER1 || 
                                status.getRole() == UserRole.PLAYER2)
                .count();
        
        long observerCount = room.getUserStatusMap().values().stream()
                .filter(status -> status.getRole() == UserRole.OBSERVER)
                .count();

        Room.UserStatus userStatus = new Room.UserStatus();
        userStatus.setReady(false);

        // 역할 할당 로직
        if (playerCount < FIXED_PLAYERS) {
            if (room.getUserStatusMap().values().stream()
                    .noneMatch(status -> status.getRole() == UserRole.PLAYER1)) {
                userStatus.setRole(UserRole.PLAYER1);
            } else {
                userStatus.setRole(UserRole.PLAYER2);
            }
        } else if (observerCount < room.getMaxObservers()) {
            userStatus.setRole(UserRole.OBSERVER);
        } else {
            throw new RoomFullException("더 이상 입장할 수 없습니다. (최대 인원: 플레이어 " + 
                FIXED_PLAYERS + "명, 관전자 " + room.getMaxObservers() + "명)");
        }

        room.getUserStatusMap().put(nickname, userStatus);
        roomRepository.save(room);
        
        // 입장 알림 전송
        Map<String, Object> joinNotification = new HashMap<>();
        joinNotification.put("nickname", nickname);
        joinNotification.put("role", userStatus.getRole());
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/userJoined", joinNotification);
        
        logger.debug("User {} joined room {} as {}", nickname, roomId, userStatus.getRole());
    }

    /**
     * 방에서 나가기 메서드
     */
    @Transactional
    public boolean leaveRoom(Room room, User user) {
        synchronized (room) {
            String userNickname = user.getNickname();
            Room.UserStatus leavingUserStatus = room.getUserStatusMap().get(userNickname);
            boolean wasPlayer1 = leavingUserStatus != null && leavingUserStatus.getRole() == UserRole.PLAYER1;
            boolean wasPlayer2 = leavingUserStatus != null && leavingUserStatus.getRole() == UserRole.PLAYER2;
            boolean wasHost = room.getHost() != null && room.getHost().getNickname().equals(userNickname);

            boolean removed = room.removeUserByNickname(userNickname);

            if (removed) {
                if (room.getUserStatusMap().isEmpty()) {
                    roomRepository.delete(room);
                    messagingTemplate.convertAndSend("/topic/roomDeleted", room.getId());
                    logger.debug("Room {} deleted because no users left.", room.getId());
                } else {
                    if (wasPlayer1) {
                        promoteNextPlayerToPlayer1(room);
                    } else if (wasPlayer2) {
                        // Player2가 나갔을 때 Observer들에게 알림
                        notifyPlayer2Left(room);
                    }
                    
                    if (wasHost) {
                        transferHostToPlayer1(room);
                    }

                    roomRepository.save(room);
                    broadcastRoomList();
                }
                return true;
            }
            return false;
        }
    }
    
    @Transactional
    public void handlePromotionRequest(Room room, User user) {
        synchronized (room) {
            // Player2 자리가 비어있는지 확인
            boolean hasPlayer2 = room.getUserStatusMap().values().stream()
                    .anyMatch(status -> status.getRole() == UserRole.PLAYER2);
            
            if (!hasPlayer2) {
                Room.UserStatus userStatus = room.getUserStatusMap().get(user.getNickname());
                if (userStatus != null && userStatus.getRole() == UserRole.OBSERVER) {
                    // Observer를 Player2로 승격
                    userStatus.setRole(UserRole.PLAYER2);
                    userStatus.setReady(false);
                    
                    try {
                        roomRepository.save(room);
                        
                        // 승격 알림 전송
                        Map<String, Object> promotionNotification = new HashMap<>();
                        promotionNotification.put("roomId", room.getId());
                        promotionNotification.put("nickname", user.getNickname());
                        promotionNotification.put("newRole", UserRole.PLAYER2);
                        messagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/playerPromotion", 
                            promotionNotification);
                        
                        logger.debug("Promoted observer {} to PLAYER2 in room {}", 
                            user.getNickname(), room.getId());
                    } catch (Exception e) {
                        logger.error("Failed to save room after promotion: {}", e.getMessage());
                        throw new RuntimeException("Failed to promote observer to player", e);
                    }
                }
            }
        }
    }
    
    private void notifyPlayer2Left(Room room) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("roomId", room.getId());
        notification.put("message", "Player2 position is now available");
        messagingTemplate.convertAndSend("/topic/room/" + room.getId() + "/player2Left", notification);
    }

    private void promoteNextPlayerToPlayer1(Room room) {
        synchronized (room) {
            Optional<Map.Entry<String, Room.UserStatus>> nextPlayer = room.getUserStatusMap().entrySet().stream()
                    .filter(entry -> entry.getValue().getRole() == UserRole.PLAYER2)
                    .findFirst();

            if (nextPlayer.isPresent()) {
                String nextPlayerNickname = nextPlayer.get().getKey();
                updateUserRole(room, nextPlayerNickname, UserRole.PLAYER1);
                
                promoteObserverToPlayer2(room);

                try {
                    roomRepository.save(room);
                    logger.debug("Updated room {} with new player roles in database", room.getId());
                } catch (Exception e) {
                    logger.error("Failed to save updated player roles for room {}: {}", room.getId(), e.getMessage());
                    throw new RuntimeException("Failed to update player roles in database", e);
                }
            }
        }
    }

    private void promoteObserverToPlayer2(Room room) {
        Optional<Map.Entry<String, Room.UserStatus>> firstObserver = room.getUserStatusMap().entrySet().stream()
                .filter(entry -> entry.getValue().getRole() == UserRole.OBSERVER)
                .findFirst();

        if (firstObserver.isPresent()) {
            String observerNickname = firstObserver.get().getKey();
            updateUserRole(room, observerNickname, UserRole.PLAYER2);
        }
    }

    private void updateUserRole(Room room, String nickname, UserRole newRole) {
        Room.UserStatus status = room.getUserStatusMap().get(nickname);
        if (status != null) {
            status.setRole(newRole);
            status.setReady(false);  // 역할이 변경되면 ready 상태 초기화
            sendRoleChangeNotification(room.getId(), nickname, newRole);
            logger.debug("Updated user {} role to {}", nickname, newRole);
        }
    }

    private void sendRoleChangeNotification(String roomId, String nickname, UserRole newRole) {
        Map<String, Object> roleUpdate = new HashMap<>();
        roleUpdate.put("roomId", roomId);
        roleUpdate.put("player", nickname);
        roleUpdate.put("newRole", newRole);
        roleUpdate.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/playerRoleChanged", roleUpdate);
    }

    private void transferHostToPlayer1(Room room) {
        Optional<Map.Entry<String, Room.UserStatus>> newPlayer1 = room.getUserStatusMap().entrySet().stream()
                .filter(entry -> entry.getValue().getRole() == UserRole.PLAYER1)
                .findFirst();
        
        if (newPlayer1.isPresent()) {
            User newHost = userService.findByNickname(newPlayer1.get().getKey());
            room.setHost(newHost);
            logger.debug("New host set to {}", newHost.getNickname());
        } else {
            // Player1이 없는 경우 첫 번째 사용자를 호스트로 설정
            String newHostNickname = room.getAllUserNicknames().iterator().next();
            User newHost = userService.findByNickname(newHostNickname);
            room.setHost(newHost);
            logger.debug("No Player1 found, setting first user as host: {}", newHost.getNickname());
        }
    }

    /**
     * 모든 방 가져오기
     */
    public Collection<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    /**
     * 특정 방 가져오기
     */
    public Room getRoomById(String roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }

    /**
     * 방 목록 브로드캐스트
     */
    private void broadcastRoomList() {
        Collection<Room> rooms = getAllRooms();
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

        messagingTemplate.convertAndSend("/topic/roomList", roomList);
    }

    @Transactional
    public void saveRoom(Room room) {
        roomRepository.save(room);
    }
    
    
}