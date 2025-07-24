package com.acme.insurance.telematics.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record TelematicsMessage(
    @JsonProperty("policy_id") String policyId,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("speed_mph") double speedMph,
    @JsonProperty("sensors") Sensors sensors,
    @JsonProperty("g_force") double gForce
) {
    
    public static TelematicsMessage create(String policyId, double speedMph, Sensors sensors, double gForce) {
        return new TelematicsMessage(
            policyId,
            Instant.now().toString(),
            speedMph,
            sensors,
            gForce
        );
    }
}