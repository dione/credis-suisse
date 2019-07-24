package com.test;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "com.test.events-correlator")
@Component
@Data
public class EventsCorrelatorProperties {
    private Long maxDuration;
}
