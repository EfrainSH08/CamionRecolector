package com.curso.camion.models;

import java.util.Map;

public class Avisos {
    private String avisos;
    private Map<String, String> horarios;

    public Avisos() {
        // Constructor vacío requerido para Firestore
    }

    public Avisos(String avisos, Map<String, String> horarios) {
        this.avisos = avisos;
        this.horarios = horarios;
    }

    public String getAvisos() {
        return avisos;
    }

    public void setAvisos(String avisos) {
        this.avisos = avisos;
    }

    public Map<String, String> getHorarios() {
        return horarios;
    }

    public void setHorarios(Map<String, String> horarios) {
        this.horarios = horarios;
    }
}
