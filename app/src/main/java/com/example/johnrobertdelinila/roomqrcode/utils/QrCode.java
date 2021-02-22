package com.example.johnrobertdelinila.roomqrcode.utils;

public class QrCode {
    private Boolean isDeactivated;
    private String room;

    public QrCode() {}

    public QrCode(Boolean isDeactivated, String room) {
        this.isDeactivated = isDeactivated;
        this.room = room;
    }

    public Boolean getDeactivated() {
        return isDeactivated;
    }

    public void setDeactivated(Boolean deactivated) {
        isDeactivated = deactivated;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}
