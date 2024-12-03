package com.hoit.checkers.dto;

import jakarta.validation.constraints.NotBlank;

public class GuestLoginRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    // Getters and Setters

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
