package com.hoit.checkers.model;

import jakarta.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
@Table(name = "rooms")
public class Room {
	@Transient

	private GameState gameState;
	
    private static final Logger logger = LoggerFactory.getLogger(Room.class);

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int maxUsers;

    @Column
    private String password; // 비밀방의 경우 비밀번호 저장

    @Column(nullable = false)
    private String createdByNickname;
    
    @Column(nullable = false)
    private int maxObservers;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "hostId", nullable = true) // CamelCase 유지
    private User host;

    private int userSequence = 0; // 사용자 입장 순서 관리

    // 사용자 상태 관리 (닉네임을 키로 사용)
    @ElementCollection
    @CollectionTable(name = "roomuserstatus", joinColumns = @JoinColumn(name = "roomid"))
    @MapKeyColumn(name = "nickname")
    @AttributeOverrides({
        @AttributeOverride(name = "sequence", column = @Column(name = "sequence")),
        @AttributeOverride(name = "role", column = @Column(name = "role")),
        @AttributeOverride(name = "isReady", column = @Column(name = "isready"))
    })
    private Map<String, UserStatus> userStatusMap = new HashMap<>();

    // 내부 클래스
    @Embeddable
    public static class UserStatus {
        private int sequence; // 입장 순서

        @Enumerated(EnumType.STRING)
        private UserRole role;

        private boolean isReady;

        // 기본 생성자
        public UserStatus() {}

        public UserStatus(int sequence, UserRole role) {
            this.sequence = sequence;
            this.role = role;
            this.isReady = false;
        }

        // Getters and Setters
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }

        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }

        public boolean isReady() { return isReady; }
        public void setReady(boolean ready) { isReady = ready; }
    }

    // 기본 생성자
    public Room() {
        this.id = UUID.randomUUID().toString();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) {
        if (maxUsers <= 0) {
            throw new IllegalArgumentException("Max users must be greater than 0");
        }
        this.maxUsers = maxUsers;
    }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCreatedByNickname() { return createdByNickname; }
    public void setCreatedByNickname(String createdByNickname) { this.createdByNickname = createdByNickname; }
    public User getHost() { return host; }
    public void setHost(User host) { this.host = host; }
    public Map<String, UserStatus> getUserStatusMap() { return userStatusMap; }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    // 현재 사용자 수 반환
    public int getCurrentUsers() {
        return userStatusMap.size();
    }

    // 비밀방 여부 확인
    public boolean isPrivateRoom() {
        return password != null && !password.isEmpty();
    }

    /**
     * 사용자 추가 메서드
     * @param nickname 사용자 닉네임
     * @param user User 객체
     * @return 성공 여부
     */
    public boolean addUserByNickname(String nickname, User user) {
        synchronized (this) {
            if (userStatusMap.containsKey(nickname)) {
                return true; // 이미 방에 있는 사용자
            }

            if (userStatusMap.size() >= maxUsers) {
                return false; // 방이 꽉 찼음
            }

            // 입장 순서 증가
            userSequence++;

            // 역할 할당
            UserRole role;
            if (userStatusMap.values().stream().noneMatch(status -> status.getRole() == UserRole.PLAYER1)) {
                role = UserRole.PLAYER1;
            } else if (userStatusMap.values().stream().noneMatch(status -> status.getRole() == UserRole.PLAYER2)) {
                role = UserRole.PLAYER2;
            } else {
                role = UserRole.OBSERVER;
            }

            // 사용자 상태 생성
            UserStatus status = new UserStatus(userSequence, role);

            userStatusMap.put(nickname, status);

            // 호스트 설정
            if (host == null) {
                this.host = user;
                logger.debug("Host set to user: {}", nickname);
            }

            logger.debug("Added user to room: {}", nickname);
            return true;
        }
    }

    /**
     * 사용자 제거 메서드
     * @param nickname 사용자 닉네임
     * @return 성공 여부
     */
    public boolean removeUserByNickname(String nickname) {
        synchronized (this) {
            UserStatus removedStatus = userStatusMap.remove(nickname);
            if (removedStatus != null) {
                // 호스트가 나간 경우 호스트 이전
                if (host != null && host.getNickname().equals(nickname)) {
                    transferHost();
                }
                logger.debug("Removed user from room: {}", nickname);
                return true;
            }
            return false;
        }
    }

    /**
     * 호스트 이전 메서드
     */
    private void transferHost() {
        synchronized (this) {
            Optional<Map.Entry<String, UserStatus>> nextHostEntry = userStatusMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(e -> e.getValue().getSequence()))
                    .findFirst();

            if (nextHostEntry.isPresent()) {
                String nextHostNickname = nextHostEntry.get().getKey();
                User newHost = null; // 실제로는 UserService를 통해 User 객체를 가져와야 함
                // 예시:
                // newHost = userService.findByNickname(nextHostNickname).orElse(null);
                // 현재 Room 클래스에서는 UserService에 접근할 수 없으므로, 호스트 이전 로직은 RoomService에서 처리해야 합니다.
                // 따라서 이 메서드는 RoomService에서 호출할 필요가 있습니다.
                // 임시로 호스트를 null로 설정
                this.host = null;
                logger.debug("Host transferred to user: {}", nextHostNickname);
            } else {
                this.host = null;
                logger.debug("No users left in room, host is now null.");
            }
        }
    }

    /**
     * 사용자 포함 여부 확인
     * @param nickname 사용자 닉네임
     * @return 포함 여부
     */
    public boolean containsUserByNickname(String nickname) {
        synchronized (this) {
            return userStatusMap.containsKey(nickname);
        }
    }

    /**
     * 사용자 역할 반환
     * @param nickname 사용자 닉네임
     * @return UserRole
     */
    public UserRole getUserRoleByNickname(String nickname) {
        synchronized (this) {
            UserStatus status = userStatusMap.get(nickname);
            return status != null ? status.getRole() : null;
        }
    }

    /**
     * 호스트 여부 확인
     * @param nickname 사용자 닉네임
     * @return 호스트 여부
     */
    public boolean isHostByNickname(String nickname) {
        synchronized (this) {
            return host != null && host.getNickname().equals(nickname);
        }
    }

    /**
     * 사용자 준비 상태 설정
     * @param nickname 사용자 닉네임
     * @param isReady 준비 상태
     */
    public void setUserReadyStatusByNickname(String nickname, boolean isReady) {
        synchronized (this) {
            UserStatus status = userStatusMap.get(nickname);
            if (status != null) {
                status.setReady(isReady);
                logger.debug("User {} ready status set to {}", nickname, isReady);
            }
        }
    }

    /**
     * 모든 플레이어가 준비되었는지 확인
     * @return 준비 완료 여부
     */
    public boolean arePlayersReady() {
        synchronized (this) {
            return userStatusMap.values().stream()
                    .filter(status -> status.getRole() == UserRole.PLAYER1 || status.getRole() == UserRole.PLAYER2)
                    .allMatch(UserStatus::isReady);
        }
    }

    /**
     * 모든 사용자 닉네임 반환
     * @return 닉네임 목록
     */
    public Collection<String> getAllUserNicknames() {
        synchronized (this) {
            return new ArrayList<>(userStatusMap.keySet());
        }
    }

    /**
     * 플레이어 목록 반환
     * @return 플레이어 닉네임 목록
     */
    public List<String> getPlayers() {
        synchronized (this) {
            return userStatusMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getRole() == UserRole.PLAYER1 || entry.getValue().getRole() == UserRole.PLAYER2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 관전자 목록 반환
     * @return 관전자 닉네임 목록
     */
    public List<String> getSpectators() {
        synchronized (this) {
            return userStatusMap.entrySet().stream()
                    .filter(entry -> entry.getValue().getRole() == UserRole.OBSERVER)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 방 초기화
     */
    public void clearRoom() {
        synchronized (this) {
            userStatusMap.clear();
            host = null;
            userSequence = 0;
            logger.debug("Room cleared.");
        }
    }
    
    public int getMaxObservers() {
        return maxObservers;
    }
    
    public void setMaxObservers(int maxObservers) {
        if (maxObservers < 0) {
            throw new IllegalArgumentException("Max observers must not be negative");
        }
        this.maxObservers = maxObservers;
    }
}
