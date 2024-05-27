package com.curso.aplicacionmapa.models;

import com.google.firebase.firestore.GeoPoint;

public class UserLocation {
    private GeoPoint geo_point;
    private long timestamp;
    private User user;

    public UserLocation() {
        // Constructor vacío necesario para Firestore
    }

    public UserLocation(GeoPoint geo_point, long timestamp, User user) {
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        this.user = user;
    }

    public GeoPoint getGeo_point() {
        return geo_point;
    }

    public void setGeo_point(GeoPoint geo_point) {
        this.geo_point = geo_point;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
