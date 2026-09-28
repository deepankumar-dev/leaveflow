package com.hackathon.leave.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The single time source; services inject Clock instead of calling now() so tests can control time. */
@Configuration
public class ClockConfig {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Bean
    public Clock clock(LeaveProperties props) {
        Clock system = Clock.system(ZONE);
        if (props.demoNow() == null || props.demoNow().isBlank()) {
            return system;
        }
        // Demo time: keeps ticking, but starts at the configured instant (the seed's reference "now").
        Instant start = OffsetDateTime.parse(props.demoNow()).toInstant();
        return Clock.offset(system, Duration.between(Instant.now(), start));
    }
}
