package com.curso.camion.models;

public class Camion {
    private String truckId;
    private String truckName;
    private String email;
    private String password;

    public Camion() {
        // Constructor vacío necesario para Firebase
    }

    public Camion(String truckId, String truckName, String email, String password) {
        this.truckId = truckId;
        this.truckName = truckName;
        this.email = email;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
