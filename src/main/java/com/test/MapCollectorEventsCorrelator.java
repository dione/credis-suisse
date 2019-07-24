package com.test;

import com.test.db.Event;
import com.test.db.EventRepository;
import com.test.log.LogLine;
import com.test.log.LogLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Profile("simple")
public class MapCollectorEventsCorrelator implements EventsCorrelator {
    private static final Logger logger = LoggerFactory.getLogger(MapCollectorEventsCorrelator.class);

    // TODO: move dependencies injection to constructor

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LogLineParser logLineParser;

    @Autowired
    private EventsCorrelatorProperties eventsCorrelatorProperties;

    public void checkFile(Path path) {
        long maxDuration = eventsCorrelatorProperties.getMaxDuration();
        try (Stream<String> stream = Files.lines(path)) {
            stream.parallel()
                    .peek(line -> logger.debug("parsing line: {}", line))
                    .map(logLineParser::parseLine)
                    .filter(logLine -> logLine != null)
                    .peek(logLine -> logger.debug("parsed line: {}", logLine))
                    .collect(Collectors.groupingBy(LogLine::getId))
                    .entrySet()
                    .stream()
                    .parallel()
                    .map(entry -> {
                        String id = entry.getKey();
                        List<LogLine> logLines = entry.getValue();
                        Event event = new Event();

                        event.setId(id);
                        event.setHost(logLines.get(0).getHost());
                        event.setType(logLines.get(0).getType());
                        event.setDuration(Math.abs(logLines.get(0).getTimestamp() - logLines.get(1).getTimestamp()));
                        //                        event.setAlert(event.getDuration() > maxDuration);
                        return event;
                    })
                    .peek(event -> event.setAlert(event.getDuration() > maxDuration))
                    .forEach(eventRepository::save);

        } catch (IOException e) {
            logger.error("ioexception:", e);
        }
    }
}
