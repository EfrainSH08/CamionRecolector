package com.curso.aplicacionmapa.models;

public class UserHomeLocation {
    private double latitude;
    private double longitude;
    private String userId;
    private String username; // Nombre de usuario
    private String email; // Correo electrónico

    public UserHomeLocation() {
        // Constructor vacío requerido por Firestore
    }

    public UserHomeLocation(double latitude, double longitude, String userId, String username, String email) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
        this.username = username;
        this.email = email;
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

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

