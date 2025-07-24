package com.acme.insurance.telematics.service;

import com.acme.insurance.telematics.model.TelematicsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class TelematicsMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TelematicsMessageListener.class);
    
    private final CrashDetectionService crashDetectionService;
    
    public TelematicsMessageListener(CrashDetectionService crashDetectionService) {
        this.crashDetectionService = crashDetectionService;
    }
    
    @RabbitListener(queues = "${telematics.queue-name:telematics_stream}")
    public void handleTelematicsMessage(TelematicsMessage message) {
        try {
            logger.debug("Received telematics message for policy: {}", message.policyId());
            
            if (crashDetectionService.isCrashEvent(message)) {
                crashDetectionService.processCrashEvent(message);
            } else {
                crashDetectionService.processNormalTelemetry(message);
            }
            
        } catch (Exception e) {
            logger.error("Error processing telematics message: {}", e.getMessage(), e);
        }
    }
}