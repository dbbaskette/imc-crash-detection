package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MagnetometerData(
    @JsonProperty("x") double x,
    @JsonProperty("y") double y,
    @JsonProperty("z") double z,
    @JsonProperty("heading") Double heading
) {
    
    public double getMagnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }
    
    public double getHeading() {
        // Use provided heading if available, otherwise calculate from x,y components
        if (heading != null) {
            return heading;
        }
        // Calculate compass heading in degrees (0-360)
        double calculatedHeading = Math.toDegrees(Math.atan2(y, x));
        return calculatedHeading < 0 ? calculatedHeading + 360 : calculatedHeading;
    }
    
    public boolean indicatesMetalCollision(double baselineMagnitude, double threshold) {
        // Significant changes in magnetic field can indicate collision with metal objects
        double currentMagnitude = getMagnitude();
        return Math.abs(currentMagnitude - baselineMagnitude) > threshold;
    }
    
    public boolean hasHeadingData() {
        return heading != null;
    }
    
    public String getCardinalDirection() {
        double h = getHeading();
        if (h >= 337.5 || h < 22.5) return "N";
        else if (h >= 22.5 && h < 67.5) return "NE";
        else if (h >= 67.5 && h < 112.5) return "E";
        else if (h >= 112.5 && h < 157.5) return "SE";
        else if (h >= 157.5 && h < 202.5) return "S";
        else if (h >= 202.5 && h < 247.5) return "SW";
        else if (h >= 247.5 && h < 292.5) return "W";
        else return "NW";
    }
}