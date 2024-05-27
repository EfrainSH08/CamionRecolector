package com.curso.camion.models;

public class UserHomeLocation {
    private double latitude;
    private double longitude;
    private String userId;

    public UserHomeLocation() {
        // Constructor vacío requerido por Firestore
    }

    public UserHomeLocation(double latitude, double longitude, String userId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getUserId() {
        return userId;
    }
}
