package com.hackathon.leave.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loads the fixed demo data on first start (an empty database only). */
@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    private final SeedService seed;

    public DataSeeder(SeedService seed) {
        this.seed = seed;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seed.seedIfEmpty()) {
            log.info("Demo data loaded: log in as e.g. asha@leave.demo / Password@123");
        }
    }
}
