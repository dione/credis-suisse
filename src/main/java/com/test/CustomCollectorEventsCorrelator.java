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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Stream;

@Service
@Profile("default")
public class CustomCollectorEventsCorrelator implements EventsCorrelator {
    private static final Logger logger = LoggerFactory.getLogger(CustomCollectorEventsCorrelator.class);

    // TODO: move dependencies injection to constructor
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LogLineParser logLineParser;

    @Autowired
    private EventsCorrelatorProperties eventsCorrelatorProperties;

    private Event createEvent(LogLine firstLine, LogLine secondLine) {
        Event event = new Event();

        event.setId(firstLine.getId());
        event.setHost(firstLine.getHost());
        event.setType(firstLine.getType());

        event.setDuration(Math.abs(firstLine.getTimestamp() - secondLine.getTimestamp()));
        event.setAlert(event.getDuration() > eventsCorrelatorProperties.getMaxDuration());
        return event;
    }

    // TODO: maybe extend Map?
    private void addLineToMapOrSaveEvent(Map<String, LogLine> map, LogLine line) {
        LogLine prevLine = map.putIfAbsent(line.getId(), line);
        if (prevLine != null) {
            map.remove(prevLine.getId());
            // maybe save in batch?
            saveEvent(createEvent(prevLine, line));
        }
    }

    private List<CompletableFuture> futures = new ArrayList<>();

    void saveEvent(Event event) {
        futures.add(CompletableFuture.runAsync(() -> {
            logger.debug("saving event: {}", event);
            eventRepository.save(event);
        }));
    }

    public void checkFile(Path path) {
        try (Stream<String> stream = Files.lines(path)) {
            Map<String, LogLine> unmachedLines =
                    stream
                            .parallel()
                            .peek(line -> logger.debug("parsing line: {}", line))
                            .map(logLineParser::parseLine)
                            .filter(logLine -> logLine != null)
                            .peek(logLine -> logger.debug("parsed line: {}", logLine))
                            .collect(Collector.of(
                                    // TODO: move to external class
                                    () -> new HashMap<String, LogLine>(),
                                    (acc, line) -> addLineToMapOrSaveEvent(acc, line),
                                    (left, right) -> {
                                        left.values().forEach(line -> addLineToMapOrSaveEvent(right, line));
                                        return right;
                                    }
                            ));
            if (unmachedLines.size() > 0) {
                logger.error("some lines were not matched: {}", unmachedLines);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        } catch (IOException e) {
            logger.error("ioexception:", e);
        }
    }
}
