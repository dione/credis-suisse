package com.test.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LogLineParser {
    private static final Logger logger = LoggerFactory.getLogger(LogLineParser.class);

    private static ObjectReader objectReader = new ObjectMapper().readerFor(LogLine.class);

    public LogLine parseLine(String text) {
        try {
            return objectReader.readValue(text);
        } catch (IOException e) {
            logger.error("parsing error", e);
            return null;
        }
    }
}
