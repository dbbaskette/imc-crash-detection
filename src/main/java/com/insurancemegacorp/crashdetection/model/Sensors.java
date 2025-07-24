package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Sensors(
    @JsonProperty("gps") GpsData gps,
    @JsonProperty("accelerometer") AccelerometerData accelerometer,
    @JsonProperty("gyroscope") GyroscopeData gyroscope,
    @JsonProperty("magnetometer") MagnetometerData magnetometer,
    @JsonProperty("barometric_pressure") Double barometricPressure,
    @JsonProperty("device") DeviceInfo device
) {
    
    public boolean isAtHighAltitude() {
        // Standard atmospheric pressure at sea level is 1013.25 hPa
        // Pressure drops approximately 12 hPa per 100m elevation
        return barometricPressure != null && barometricPressure < 900.0; // ~900m elevation
    }
    
    public double getEstimatedAltitudeMeters() {
        if (barometricPressure == null) return 0.0;
        // Simplified barometric formula for altitude estimation
        return 44330 * (1 - Math.pow(barometricPressure / 1013.25, 0.1903));
    }
    
    public boolean indicatesWeatherEvent() {
        // Rapid pressure changes might indicate severe weather
        return barometricPressure != null && (barometricPressure < 980.0 || barometricPressure > 1050.0);
    }
}