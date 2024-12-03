package com.hoit.checkers.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_ready_status")
public class RoomReadyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "userId", nullable = true) // nullable=true로 설정하여 비회원 지원
    private User user;

    @ManyToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private boolean isReady;

    // 기본 생성자
    public RoomReadyStatus() {
    }

    // 모든 필드를 포함한 생성자 (선택 사항)
    public RoomReadyStatus(Room room, User user, String nickname, boolean isReady) {
        this.room = room;
        this.user = user;
        this.nickname = nickname;
        this.isReady = isReady;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isReady() {
        return isReady;
    }

    // setReady 메서드 추가
    public void setReady(boolean ready) {
        this.isReady = ready;
    }

    // 기존의 setIsReady 메서드가 있다면 제거하거나 통일
    // public void setIsReady(boolean isReady) {
    //     this.isReady = isReady;
    // }
}
