package com.example.johnrobertdelinila.roomqrcode.utils;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Upload {
    private String code, imei, key;
    private Boolean timeIn, isOnline;
    private @ServerTimestamp Date timestamp;

    public Upload() {}

    public Upload(String rawValue, String imei, Date timestamp, Boolean isTimeIn, Boolean isOnline) {
        this.code = rawValue;
        this.imei = imei;
        this.timestamp = timestamp;
        this.timeIn = isTimeIn;
        this.isOnline = isOnline;
    }

    public Boolean getOnline() {
        return isOnline;
    }

    public void setOnline(Boolean online) {
        isOnline = online;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String rawValue) {
        this.code = rawValue;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getTimeIn() {
        return timeIn;
    }

    public void setTimeIn(Boolean isTimeIn) {
        timeIn = isTimeIn;
    }

    public void removeKey() {
        if (key != null) {
            key = null;
        }
    }

}
