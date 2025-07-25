package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GyroscopeData(
    @JsonProperty("x") double pitch,
    @JsonProperty("y") double roll,
    @JsonProperty("z") double yaw
) {
    
    public double getMagnitude() {
        return Math.sqrt(pitch * pitch + roll * roll + yaw * yaw);
    }
    
    public boolean indicatesRollover(double threshold) {
        // Rollover detection based on roll angular velocity
        // Threshold typically around 3-5 rad/s for vehicle rollover
        return Math.abs(roll) > threshold || getMagnitude() > threshold;
    }
    
    public boolean indicatesSpinning(double threshold) {
        // Vehicle spinning if high yaw rotation
        return Math.abs(yaw) > threshold;
    }
    
    public boolean indicatesPitchingMotion(double threshold) {
        // Forward/backward pitching motion (going over hills, sudden braking)
        return Math.abs(pitch) > threshold;
    }
    
    public String getMotionType() {
        double pitchAbs = Math.abs(pitch);
        double rollAbs = Math.abs(roll);
        double yawAbs = Math.abs(yaw);
        
        if (rollAbs > 3.0) return "ROLLING";
        if (yawAbs > 2.0) return "SPINNING";
        if (pitchAbs > 2.0) return "PITCHING";
        if (getMagnitude() > 1.0) return "UNSTABLE";
        return "STABLE";
    }
    
    // Legacy methods for backward compatibility
    public double x() { return pitch; }
    public double y() { return roll; }
    public double z() { return yaw; }
}