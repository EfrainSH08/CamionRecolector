package com.curso.aplicacionmapa.models;

import com.google.firebase.firestore.GeoPoint;

public class TruckLocation {
    private GeoPoint location;
    private long timestamp;
    private String truckId;
    private String truckName;
    private String email;

    public TruckLocation() {
        // Constructor vacío requerido para Firestore
    }

    public TruckLocation(GeoPoint location, long timestamp, String truckId, String truckName, String email) {
        this.location = location;
        this.timestamp = timestamp;
        this.truckId = truckId;
        this.truckName = truckName;
        this.email = email;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTruckId() {
        return truckId;
    }

    public void setTruckId(String truckId) {
        this.truckId = truckId;
    }

    public String getTruckName() {
        return truckName;
    }

    public void setTruckName(String truckName) {
        this.truckName = truckName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
