package com.unipi.domi.bibliosilencer;

public class Biblioteca {
    /**
     * Parametri
     */
    private double longitude, latitude, averageSound;
    private String name;
    static final double eQuatorialEarthRadius = 6378.1370D;
    static final double d2r = (Math.PI / 180D);

    public Biblioteca (String name, double longitude, double latitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.averageSound = 0.00;
    }

    @Override
    public String toString(){
        String biblioString = "";
        biblioString += "NOME: " + name;
        biblioString += "\nLAT: " + latitude;
        biblioString += "\nLON: " + longitude;
        biblioString += "\nNOISE: " + String.valueOf(averageSound) + "dB";
        return biblioString;
    }

    public String getName() {
        return name;
    }

    public String getLatitude() {
        return String.valueOf(latitude);
    }

    public String getLongitude() {
        return String.valueOf(longitude);
    }

    public double getAverageSound() {
        return averageSound;
    }

    public void setAverageSound(double averageSound) {
        this.averageSound = averageSound;
    }

    public int HaversineInM(double lat1, double long1) {
        return (int) (1000D * HaversineInKM(lat1, long1));
    }

    public double HaversineInKM(double lat, double lon) {
        double dlong = (longitude - lon) * d2r;
        double dlat = (latitude - lat) * d2r;
        double a = Math.pow(Math.sin(dlat / 2D), 2D) + Math.cos(lat * d2r) * Math.cos(latitude * d2r)
                * Math.pow(Math.sin(dlong / 2D), 2D);
        double c = 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
        double d = eQuatorialEarthRadius * c;
        return d;
    }
}
