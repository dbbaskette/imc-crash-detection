package com.insurancemegacorp.crashdetection.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AccelerometerData(
    @JsonProperty("x") double x,
    @JsonProperty("y") double y,
    @JsonProperty("z") double z
) {
}