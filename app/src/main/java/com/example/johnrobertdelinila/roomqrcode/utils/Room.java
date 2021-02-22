package com.example.johnrobertdelinila.roomqrcode.utils;

import java.io.Serializable;

public class Room implements Serializable {
    private String roomName;

    public Room() {};

    public Room(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
