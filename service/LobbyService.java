package com.hoit.checkers.service;

import com.hoit.checkers.model.LobbyUser;
import com.hoit.checkers.model.User;
import com.hoit.checkers.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LobbyService {
    // ConcurrentHashMap을 사용하여 thread-safe하게 관리
    private Map<String, LobbyUser> lobbyUsers = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * 사용자를 로비에 추가하는 메서드
     * @param user 추가할 사용자 정보
     * @return 사용자가 성공적으로 추가되었는지 여부
     */
    public void addUser(LobbyUser user) {
        // 동일한 uniqueId로 사용자 정보를 업데이트하거나 추가
        lobbyUsers.put(user.getUniqueId(), user);
        broadcastUserList();
        broadcastSessionId(user.getUniqueId(), user.getNickname());
    }
    
    /**
     * 일반 사용자를 로비에 추가하는 메서드
     * @param userId 사용자 ID (예: username)
     * @param user 추가할 사용자 정보
     * @return 사용자가 성공적으로 추가되었는지 여부
     */
    public void addUser(String userId, User user) {
        LobbyUser lobbyUser = new LobbyUser(userId, user.getNickname(), true);
        addUser(lobbyUser);
    }

    /**
     * 비회원 사용자(게스트)를 로비에 추가하는 메서드
     * @param sessionId 사용자 세션 ID
     * @param user 추가할 사용자 정보
     * @return 사용자가 성공적으로 추가되었는지 여부
     */
    public void addGuestUser(String sessionId, User user) {
        LobbyUser lobbyUser = new LobbyUser(sessionId, user.getNickname(), false);
        addUser(lobbyUser);
    }

    /**
     * 회원 사용자를 게스트로 전환하는 메서드
     * @param userId 회원의 사용자 ID
     * @param sessionId 새로운 게스트의 세션 ID
     * @param user 사용자 정보
     * @return 전환이 성공했는지 여부
     */
    public void demoteMemberToGuest(String userId, String sessionId, User user) {
        if (!lobbyUsers.containsKey(userId)) {
            // 해당 사용자가 로비에 없음
            return;
        }
        // 기존 회원 정보 제거
        lobbyUsers.remove(userId);
        
        // 게스트로 전환하여 추가
        LobbyUser guestUser = new LobbyUser(sessionId, user.getNickname(), false);
        addUser(guestUser);
    }

    // 닉네임 중복 확인
    public boolean isNicknameTaken(String nickname, String uniqueId) {
        return lobbyUsers.values().stream()
                .anyMatch(user -> user.getNickname().equals(nickname) && !user.getUniqueId().equals(uniqueId));
    }

    public boolean isNicknameTaken(String nickname) {
        return lobbyUsers.values().stream()
                .anyMatch(user -> user.getNickname().equals(nickname));
    }
    /**
     * 사용자를 로비에서 제거하는 메서드
     * @param uniqueId 제거할 사용자의 고유 식별자 (userId 또는 sessionId)
     */
    public void removeUser(String uniqueId) {
        if (lobbyUsers.containsKey(uniqueId)) {
            lobbyUsers.remove(uniqueId);
            broadcastUserList(); // 사용자 제거 시 브로드캐스트
        }
    }

    /**
     * 모든 사용자를 반환하는 메서드
     * @return 모든 LobbyUser의 컬렉션
     */
    public Collection<LobbyUser> getAllUsers() {
        return lobbyUsers.values();
    }

    /**
     * 특정 식별자로 사용자 조회
     * @param uniqueId 사용자 고유 식별자 (userId 또는 sessionId)
     * @return 해당 식별자를 가진 LobbyUser
     */
    public LobbyUser getUser(String uniqueId) {
        return lobbyUsers.get(uniqueId);
    }

    /**
     * 사용자 목록 브로드캐스트
     */
    private void broadcastUserList() {
        System.out.println("Broadcasting user list...");
        messagingTemplate.convertAndSend("/topic/userList", getAllUsers());
        System.out.println("Broadcasted user list: " + getAllUsers());
    }

    /**
     * 세션 ID 브로드캐스트 메서드
     * @param uniqueId 사용자 고유 식별자
     * @param nickname 사용자 닉네임
     */
    private void broadcastSessionId(String uniqueId, String nickname) {
        System.out.println("Broadcasting sessionId for user: " + nickname + " with uniqueId: " + uniqueId);
        messagingTemplate.convertAndSendToUser(uniqueId, "/queue/sessionId", uniqueId);
    }

    /**
     * 로비에 특정 사용자(ID 기준)가 존재하는지 확인하는 메서드
     * @param uniqueId 사용자 고유 식별자
     * @return 사용자가 로비에 있는지 여부
     */
    public boolean isUserInLobby(String uniqueId) {
        return lobbyUsers.containsKey(uniqueId);
    }

    /**
     * 사용자 존재 여부 확인
     * @param uniqueId 사용자 고유 식별자
     * @return 사용자가 로비에 존재하는지 여부
     */
    public boolean containsUser(String uniqueId) {
        return lobbyUsers.containsKey(uniqueId);
    }
}
