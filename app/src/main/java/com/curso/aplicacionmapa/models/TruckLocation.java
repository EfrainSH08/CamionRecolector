package com.curso.aplicacionmapa.models;

import com.curso.aplicacionmapa.models.Camion;
import com.google.firebase.firestore.GeoPoint;

public class TruckLocation {
    private GeoPoint location;
    private long timestamp;
    private Camion camion;

    public TruckLocation() {
        // Constructor vacío requerido para Firestore
    }

    public TruckLocation(GeoPoint location, long timestamp, Camion camion) {
        this.location = location;
        this.timestamp = timestamp;
        this.camion = camion;
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

    public Camion getCamion() {
        return camion;
    }

    public void setCamion(Camion camion) {
        this.camion = camion;
    }
}
