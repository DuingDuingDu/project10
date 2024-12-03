package com.hoit.checkers.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    // 열거형 UserType 추가
    public enum UserType {
        MEMBER,
        GUEST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = true, unique = true, length = 255)
    private String guestId;

    @Column(name = "nickname", nullable = true, unique = true, length = 255)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private UserRole userRole; // 플레이어 역할을 지정하는 필드 추가

    @Column(nullable = true, columnDefinition = "int(11) default 0")
    private int wins;

    @Column(nullable = true, columnDefinition = "int(11) default 0")
    private int draws;

    @Column(nullable = true, columnDefinition = "int(11) default 0")
    private int losses;

    @Column(name = "winrate", nullable = true, columnDefinition = "double default 0")
    private double winRate;

    @Column(name = "created_at", nullable = true, updatable = false)
    private LocalDateTime createdAt;

    // 기본 생성자
    public User() {
        // JPA에서는 기본 생성자가 필요합니다.
    }

    // 게스트 유저용 생성자
    public User(String nickname) {
        this.nickname = nickname;
        this.userType = UserType.GUEST;
    }

    // 정식 회원용 생성자
    public User(String username, String password, String nickname) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.userType = UserType.MEMBER;
    }

    // Getter 및 Setter 메서드

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
        calculateWinRate();
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
        calculateWinRate();
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
        calculateWinRate();
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 'created_at' 필드는 JPA가 자동으로 관리하므로 setter는 생략

    // 엔티티가 생성될 때 'created_at' 필드 설정
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 사용자 유형에 따라 게스트 여부 판단
    public boolean isGuest() {
        return this.userType == UserType.GUEST;
    }

    // uniqueId 메서드 추가
    public String getUniqueId() {
        return this.username;
    }

    // 승률 계산 메서드 추가
    public void calculateWinRate() {
        int totalGames = wins + draws + losses;
        if (totalGames > 0) {
            this.winRate = ((double) wins / totalGames) * 100;
        } else {
            this.winRate = 0;
        }
    }

    // equals 및 hashCode 재정의 (닉네임을 기준으로)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;
        return Objects.equals(nickname, user.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickname);
    }
}
