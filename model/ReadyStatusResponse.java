package com.hoit.checkers.model;

public class ReadyStatusResponse {
    private boolean allReady;

    public ReadyStatusResponse() {
    }

    public ReadyStatusResponse(boolean allReady) {
        this.allReady = allReady;
    }

    public boolean isAllReady() {
        return allReady;
    }

    public void setAllReady(boolean allReady) {
        this.allReady = allReady;
    }
}
