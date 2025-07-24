package com.acme.insurance.telematics.service;

import com.acme.insurance.telematics.model.AccelerometerData;
import com.acme.insurance.telematics.model.GpsData;
import com.acme.insurance.telematics.model.Sensors;
import com.acme.insurance.telematics.model.TelematicsMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelematicsMessageListenerTest {
    
    @Mock
    private CrashDetectionService crashDetectionService;
    
    private TelematicsMessageListener messageListener;
    
    @BeforeEach
    void setUp() {
        messageListener = new TelematicsMessageListener(crashDetectionService);
    }
    
    @Test
    void shouldProcessCrashEvent() {
        TelematicsMessage crashMessage = createMessage("TEST-POLICY", 35.0, 6.0, 5.5, 1.0, 8.2);
        when(crashDetectionService.isCrashEvent(crashMessage)).thenReturn(true);
        
        messageListener.handleTelematicsMessage(crashMessage);
        
        verify(crashDetectionService).isCrashEvent(crashMessage);
        verify(crashDetectionService).processCrashEvent(crashMessage);
        verify(crashDetectionService, never()).processNormalTelemetry(any());
    }
    
    @Test
    void shouldProcessNormalTelemetry() {
        TelematicsMessage normalMessage = createMessage("TEST-POLICY", 30.0, 0.2, -0.1, 0.98, 1.0);
        when(crashDetectionService.isCrashEvent(normalMessage)).thenReturn(false);
        
        messageListener.handleTelematicsMessage(normalMessage);
        
        verify(crashDetectionService).isCrashEvent(normalMessage);
        verify(crashDetectionService).processNormalTelemetry(normalMessage);
        verify(crashDetectionService, never()).processCrashEvent(any());
    }
    
    @Test
    void shouldHandleProcessingException() {
        TelematicsMessage message = createMessage("TEST-POLICY", 30.0, 0.2, -0.1, 0.98, 1.0);
        when(crashDetectionService.isCrashEvent(message)).thenThrow(new RuntimeException("Processing error"));
        
        // Should not throw exception
        messageListener.handleTelematicsMessage(message);
        
        verify(crashDetectionService).isCrashEvent(message);
        // Verify no further processing occurs after exception
        verify(crashDetectionService, never()).processNormalTelemetry(any());
        verify(crashDetectionService, never()).processCrashEvent(any());
    }
    
    private TelematicsMessage createMessage(String policyId, double speed, double accelX, double accelY, double accelZ, double gForce) {
        GpsData gps = new GpsData(40.7128, -74.0060);
        AccelerometerData accelerometer = new AccelerometerData(accelX, accelY, accelZ);
        Sensors sensors = new Sensors(gps, accelerometer);
        return new TelematicsMessage(policyId, "2024-07-24T10:30:00Z", speed, sensors, gForce);
    }
}