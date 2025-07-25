package com.insurancemegacorp.crashdetection.service;

import com.insurancemegacorp.crashdetection.model.AccelerometerData;
import com.insurancemegacorp.crashdetection.model.DeviceInfo;
import com.insurancemegacorp.crashdetection.model.GpsData;
import com.insurancemegacorp.crashdetection.model.GyroscopeData;
import com.insurancemegacorp.crashdetection.model.MagnetometerData;
import com.insurancemegacorp.crashdetection.model.Sensors;
import com.insurancemegacorp.crashdetection.model.TelematicsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CrashDetectionServiceTest {
    
    private CrashDetectionService crashDetectionService;
    
    @BeforeEach
    void setUp() {
        crashDetectionService = new CrashDetectionService();
        ReflectionTestUtils.setField(crashDetectionService, "gForceThreshold", 4.0);
        ReflectionTestUtils.setField(crashDetectionService, "speedThreshold", 5.0);
    }
    
    @Test
    void shouldDetectHighGForceCrash() {
        TelematicsMessage crashMessage = createMessage("TEST-POLICY", 35.0, 6.0, 5.5, 1.0, 8.2);
        
        boolean isCrash = crashDetectionService.isCrashEvent(crashMessage);
        
        assertThat(isCrash).isTrue();
    }
    
    @Test
    void shouldDetectSuddenStopCrash() {
        TelematicsMessage crashMessage = createMessage("TEST-POLICY", 2.0, 1.5, 1.2, 1.0, 2.1);
        
        boolean isCrash = crashDetectionService.isCrashEvent(crashMessage);
        
        assertThat(isCrash).isTrue();
    }
    
    @Test
    void shouldNotDetectNormalDriving() {
        TelematicsMessage normalMessage = createMessage("TEST-POLICY", 30.0, 0.2, -0.1, 0.98, 1.0);
        
        boolean isCrash = crashDetectionService.isCrashEvent(normalMessage);
        
        assertThat(isCrash).isFalse();
    }
    
    @Test
    void shouldNotDetectLowSpeedWithLowGForce() {
        TelematicsMessage lowSpeedMessage = createMessage("TEST-POLICY", 3.0, 0.5, 0.3, 0.9, 1.1);
        
        boolean isCrash = crashDetectionService.isCrashEvent(lowSpeedMessage);
        
        assertThat(isCrash).isFalse();
    }
    
    private TelematicsMessage createMessage(String policyId, double speed, double accelX, double accelY, double accelZ, double gForce) {
        GpsData gps = new GpsData(40.7128, -74.0060, 0.0, 15.0, 45.0, 5.0, 8, "2024-07-24T10:30:00Z");
        AccelerometerData accelerometer = new AccelerometerData(accelX, accelY, accelZ);
        GyroscopeData gyroscope = new GyroscopeData(0.0, 0.0, 0.0);
        MagnetometerData magnetometer = new MagnetometerData(23.0, -12.0, 45.0, 45.0);
        DeviceInfo device = new DeviceInfo(0.85, -65, "portrait", true, false);
        Sensors sensors = new Sensors(gps, accelerometer, gyroscope, magnetometer, 1013.25, device);
        return new TelematicsMessage(policyId, "VIN123456789", "2024-07-24T10:30:00Z", speed, "Main St", gForce, sensors);
    }
}