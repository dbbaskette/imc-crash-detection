package com.insurancemegacorp.crashdetection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
public class MessageConverterConfig {

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        
        // Configure to ignore type headers and use target type from method signature
        converter.setStrictContentTypeMatch(false);
        
        return converter;
    }
}
