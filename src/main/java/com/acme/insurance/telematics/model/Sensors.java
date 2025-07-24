package com.acme.insurance.telematics.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Sensors(
    @JsonProperty("gps") GpsData gps,
    @JsonProperty("accelerometer") AccelerometerData accelerometer
) {
}