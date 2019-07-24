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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collector;
import java.util.stream.Stream;

@Service
@Profile("default")
@Transactional
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
        // I assume that host in events with same id are always equals
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
            sendEvent(createEvent(prevLine, line));
        }
    }

    BlockingQueue<Message> blockingQueue = new ArrayBlockingQueue<>(100);

    private void sendEvent(Event event) {
        try {
            blockingQueue.put(Message.event(event));
        } catch (InterruptedException ex) {
            logger.error(ex.getMessage());
        }
    }

    private void shutdownQueue() {
        try {
            blockingQueue.put(Message.shutdown());
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    public void checkFile(Path path) {
        try (Stream<String> stream = Files.lines(path)) {
            Runnable producer = () -> {

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
                shutdownQueue();
            };
            Runnable consumer = () -> {
                try {
                    while (true) {
                        logger.debug("waiting for events");
                        Message message = blockingQueue.take();
                        if (message.isShutdown()) {
                            logger.debug("shutting down");
                            break;
                        }
                        Event e = message.getEvent();

                        logger.debug("saving event", e);
                        eventRepository.save(e);
                    }
                } catch (InterruptedException ex) {
                    logger.error(ex.getMessage());
                }
            };

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(producer);

            consumer.run();

            executorService.shutdown();
        } catch (IOException e) {
            logger.error("ioexception:", e);
        }
    }
}
