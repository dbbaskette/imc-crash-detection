package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeviceInfo(
    @JsonProperty("battery_level") Double batteryLevel, // Now as decimal (0.0-1.0)
    @JsonProperty("signal_strength") Integer signalStrength,
    @JsonProperty("orientation") String orientation,
    @JsonProperty("screen_on") Boolean screenOn,
    @JsonProperty("charging") Boolean charging
) {
    
    public boolean isLowBattery() {
        return batteryLevel != null && batteryLevel < 0.20; // Below 20%
    }
    
    public boolean isCriticalBattery() {
        return batteryLevel != null && batteryLevel < 0.10; // Below 10%
    }
    
    public boolean isWeakSignal() {
        return signalStrength != null && signalStrength < -80; // dBm
    }
    
    public boolean isInPortraitMode() {
        return "portrait".equalsIgnoreCase(orientation);
    }
    
    public boolean isInLandscapeMode() {
        return "landscape".equalsIgnoreCase(orientation);
    }
    
    public boolean isScreenActive() {
        return screenOn != null && screenOn;
    }
    
    public boolean isCharging() {
        return charging != null && charging;
    }
    
    public int getBatteryPercentage() {
        return batteryLevel != null ? (int) Math.round(batteryLevel * 100) : 0;
    }
    
    public String getDeviceHealthStatus() {
        if (isCriticalBattery() && !isCharging()) return "CRITICAL_BATTERY";
        if (isLowBattery() && !isCharging()) return "LOW_BATTERY";
        if (isWeakSignal()) return "WEAK_SIGNAL";
        return "HEALTHY";
    }
    
    public String getSignalQuality() {
        if (signalStrength == null) return "UNKNOWN";
        if (signalStrength >= -50) return "EXCELLENT";
        if (signalStrength >= -60) return "GOOD";
        if (signalStrength >= -70) return "FAIR";
        if (signalStrength >= -80) return "POOR";
        return "VERY_POOR";
    }
}