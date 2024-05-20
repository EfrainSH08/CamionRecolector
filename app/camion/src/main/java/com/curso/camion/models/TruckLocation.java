package com.curso.camion.models;

public class TruckLocation {
    private double latitude;
    private double longitude;
    private String truckId;

    public TruckLocation() {
        // Constructor vacío requerido por Firestore
    }

    public TruckLocation(double latitude, double longitude, String truckId) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.truckId = truckId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTruckId() {
        return truckId;
    }

    public void setTruckId(String truckId) {
        this.truckId = truckId;
    }
}
