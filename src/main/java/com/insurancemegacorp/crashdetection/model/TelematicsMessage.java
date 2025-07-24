package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TelematicsMessage(
    @JsonProperty("policy_id") String policyId,
    @JsonProperty("vin") String vin,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("speed_mph") double speedMph,
    @JsonProperty("current_street") String currentStreet,
    @JsonProperty("g_force") double gForce,
    @JsonProperty("sensors") Sensors sensors
) {
    
    // Use the pre-calculated G-force from the message, but provide backup calculation
    public double getCalculatedGForce() {
        // Calculate G-force from accelerometer data as backup/validation
        double accelMagnitude = Math.sqrt(
            sensors.accelerometer().x() * sensors.accelerometer().x() +
            sensors.accelerometer().y() * sensors.accelerometer().y() +
            sensors.accelerometer().z() * sensors.accelerometer().z()
        );
        return accelMagnitude / 9.81;
    }
    
    public double lateralGForce() {
        // Calculate lateral G-force (X and Y axes, excluding gravity on Z)
        double lateralAccel = Math.sqrt(
            sensors.accelerometer().x() * sensors.accelerometer().x() +
            sensors.accelerometer().y() * sensors.accelerometer().y()
        );
        return lateralAccel / 9.81;
    }
    
    public double verticalGForce() {
        // Calculate vertical G-force (Z axis, accounting for gravity)
        return Math.abs((sensors.accelerometer().z() - 9.81) / 9.81);
    }
    
    public boolean indicatesRollover() {
        // Rollover detection using gyroscope data
        return sensors.gyroscope().indicatesRollover(3.0); // 3 rad/s threshold
    }
    
    public boolean indicatesSpinning() {
        // Vehicle spinning detection
        return sensors.gyroscope().indicatesSpinning(2.0); // 2 rad/s Z-axis threshold
    }
    
    public double getHeading() {
        // Get compass heading from magnetometer
        return sensors.magnetometer().getHeading();
    }
    
    public static TelematicsMessage create(String policyId, String vin, double speedMph, String currentStreet, double gForce, Sensors sensors) {
        return new TelematicsMessage(
            policyId,
            vin,
            Instant.now().toString(),
            speedMph,
            currentStreet,
            gForce,
            sensors
        );
    }
}