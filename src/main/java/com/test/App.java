package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Paths;

@SpringBootApplication
@Transactional
public class App implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(String... args) throws IOException {
        String filename = args[0];
        logger.info("maximal event duration: {} ms", eventsCorrelatorProperties.getMaxDuration());
        logger.info("logfile: {}", filename);
        // TODO: maybe add some nice checking if file exists and we have permissions to read it?
        eventsCorrelator.checkFile(Paths.get(filename));
        logger.info("finished!!!");
    }

    @Autowired
    private EventsCorrelator eventsCorrelator;

    @Autowired
    private EventsCorrelatorProperties eventsCorrelatorProperties;
}

