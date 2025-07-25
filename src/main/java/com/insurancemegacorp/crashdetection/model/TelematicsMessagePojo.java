package com.insurancemegacorp.crashdetection.model;

import java.io.Serializable;

/**
 * POJO version of TelematicsMessage for Spark serialization compatibility.
 * Spark requires JavaBean-style POJOs with getters/setters for proper serialization.
 */
public class TelematicsMessagePojo implements Serializable {
    private String policyId;
    private String vin;
    private String timestamp;
    private double speedMph;
    private String currentStreet;
    private double gForce;
    private Sensors sensors;

    // Default constructor required by Spark
    public TelematicsMessagePojo() {}

    // Constructor to convert from TelematicsMessage record
    public TelematicsMessagePojo(TelematicsMessage message) {
        this.policyId = message.policyId();
        this.vin = message.vin();
        this.timestamp = message.timestamp();
        this.speedMph = message.speedMph();
        this.currentStreet = message.currentStreet();
        this.gForce = message.gForce();
        this.sensors = message.sensors();
    }

    // Getters and setters required by Spark
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getSpeedMph() { return speedMph; }
    public void setSpeedMph(double speedMph) { this.speedMph = speedMph; }

    public String getCurrentStreet() { return currentStreet; }
    public void setCurrentStreet(String currentStreet) { this.currentStreet = currentStreet; }

    public double getGForce() { return gForce; }
    public void setGForce(double gForce) { this.gForce = gForce; }

    public Sensors getSensors() { return sensors; }
    public void setSensors(Sensors sensors) { this.sensors = sensors; }
}
