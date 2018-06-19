package com.unipi.domi.bibliosilencer;

public class Biblioteca {
    /**
     * Parametri
     */
    private double longitude;
    private double latitude;
    private String name;

    public Biblioteca (String name, double longitude, double latitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public String getLatitude() {return String.valueOf(latitude);}

    public String getLongitude() {return String.valueOf(longitude);}
}
