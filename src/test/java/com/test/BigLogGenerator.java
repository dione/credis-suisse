package com.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.test.log.LogLine;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class BigLogGenerator {
    private static final ObjectWriter objectWriter = new ObjectMapper().writerFor(LogLine.class);
    private Random random = new Random(42);
    private Set<String> usedEventIds = new HashSet<>();

    public void generateEvent(Writer fileWriter) throws IOException {
        String eventId = randomString(2, 10);
        while (usedEventIds.contains(eventId)) {
            eventId = randomString(2, 10);
        }
        usedEventIds.add(eventId);

        LogLine startLineLog = new LogLine();
        startLineLog.setId(eventId);
        startLineLog.setHost("hostname");
        startLineLog.setState("state");
        startLineLog.setType("STARTED");
        startLineLog.setTimestamp(1000001L);
        LogLine endLineLog = new LogLine();
        endLineLog.setId(eventId);
        endLineLog.setHost("hostname");
        endLineLog.setState("state");
        endLineLog.setType("FINISHED");
        endLineLog.setTimestamp(1000101L);

        fileWriter.append(objectWriter.writeValueAsString(startLineLog));
        fileWriter.append("\n");
        fileWriter.append(objectWriter.writeValueAsString(endLineLog));
        fileWriter.append("\n");
    }

    public File generateEventFile(int eventsNumber) throws IOException {
        File file = File.createTempFile("test-file-", ".json");
        file.deleteOnExit();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < eventsNumber; ++i)
                generateEvent(out);
        }
        return file;
    }

    private String randomString(int minLengthInclusive, int maxLengthExclusive) {
        int count = minLengthInclusive + random.nextInt(maxLengthExclusive);
        return RandomStringUtils.random(count, 0, 0, true, true, null, random);
    }
}
