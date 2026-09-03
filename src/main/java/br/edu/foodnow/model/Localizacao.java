package br.edu.foodnow.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Localizacao {
    private double latitude;
    private double longitude;

    protected Localizacao() {
    }

    public Localizacao(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas inválidas");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
