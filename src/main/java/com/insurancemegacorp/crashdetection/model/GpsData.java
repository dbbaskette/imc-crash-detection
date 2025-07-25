package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GpsData(
    @JsonProperty("lat") double latitude,
    @JsonProperty("lon") double longitude,
    @JsonProperty("altitude") Double altitude,
    @JsonProperty("speed") Double speedMs,
    @JsonProperty("bearing") Double bearing,
    @JsonProperty("accuracy") Double accuracy,
    @JsonProperty("satellite_count") Integer satelliteCount,
    @JsonProperty("fix_time") String gpsFixTime
) {
    
    public double getSpeedMph() {
        return speedMs != null ? speedMs * 2.237 : 0.0; // Convert m/s to mph
    }
    
    public boolean isHighAccuracy() {
        return accuracy != null && accuracy <= 5.0; // Within 5 meters
    }
    
    public boolean hasGoodSatelliteReception() {
        return satelliteCount != null && satelliteCount >= 4;
    }
    
    public boolean isAtHighAltitude() {
        return altitude != null && altitude > 1000.0; // Above 1000m
    }
    
    public String getGpsQuality() {
        if (!hasGoodSatelliteReception()) return "POOR";
        if (!isHighAccuracy()) return "MODERATE";
        return "EXCELLENT";
    }
    
    public double distanceTo(GpsData other) {
        // Haversine formula for distance calculation
        double R = 6371000; // Earth's radius in meters
        double lat1Rad = Math.toRadians(this.latitude);
        double lat2Rad = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);
        
        double a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
                  Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                  Math.sin(deltaLon/2) * Math.sin(deltaLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        
        return R * c;
    }
}