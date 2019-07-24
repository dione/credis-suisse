package com.test;

import com.test.db.Event;
import com.test.db.EventRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AppTestConfiguration.class)
@ActiveProfiles({"default", "test"})
@AutoConfigureTestDatabase
public class AppTest {
    private static final Logger logger = LoggerFactory.getLogger(AppTest.class);

    @Autowired
    private EventsCorrelator eventsCorrelator;

    @Autowired
    private EventRepository eventRepository;

    @Before
    public void setUp() {
        eventRepository.deleteAllInBatch();
    }

    @Test
    public void when20Lines_shouldReturn10Events() throws Exception {
        BigLogGenerator bigLogGenerator = new BigLogGenerator();
        File file = bigLogGenerator.generateEventFile(10);
        eventsCorrelator.checkFile(Paths.get(file.getAbsolutePath()));
        List<Event> events = eventRepository.findAll();
        assertThat(events.size(), is(equalTo(10)));
    }

    @Test
    public void when200Lines_shouldReturn100Events() throws Exception {
        BigLogGenerator bigLogGenerator = new BigLogGenerator();
        File file = bigLogGenerator.generateEventFile(100);
        eventsCorrelator.checkFile(Paths.get(file.getAbsolutePath()));
        List<Event> events = eventRepository.findAll();
        assertThat(events.size(), is(equalTo(100)));
    }

    @Test
    @Ignore
    public void when2MLines_shouldReturn1MEvents() throws Exception {
        BigLogGenerator bigLogGenerator = new BigLogGenerator();
        File file = bigLogGenerator.generateEventFile(1_000_000);
        eventsCorrelator.checkFile(Paths.get(file.getAbsolutePath()));
        List<Event> events = eventRepository.findAll();
        assertThat(events.size(), is(equalTo(1_000_000)));
    }

    @Test
    public void whenExampleFile_shouldReturnCorrectEvents() {
        eventsCorrelator.checkFile(Paths.get("example.json"));
        List<Event> events = eventRepository.findAll().stream().sorted(Comparator.comparing(Event::getId)).collect(Collectors.toList());
        assertThat(events.size(), is(equalTo(3)));

        assertThat(events.get(0).getId(), is(equalTo("scsmbstgra")));
        assertThat(events.get(0).getDuration(), is(equalTo(5L)));
        assertThat(events.get(0).getType(), is(equalTo("APPLICATION_LOG")));
        assertThat(events.get(0).getHost(), is(equalTo("12345")));
        assertThat(events.get(0).isAlert(), is(equalTo(true)));

        assertThat(events.get(1).getId(), is(equalTo("scsmbstgrb")));
        assertThat(events.get(1).getDuration(), is(equalTo(3L)));
        assertThat(events.get(1).getType(), is(equalTo(null)));
        assertThat(events.get(1).getHost(), is(equalTo(null)));
        assertThat(events.get(1).isAlert(), is(equalTo(false)));

        assertThat(events.get(2).getId(), is(equalTo("scsmbstgrc")));
        assertThat(events.get(2).getDuration(), is(equalTo(8L)));
        assertThat(events.get(2).getType(), is(equalTo(null)));
        assertThat(events.get(2).getHost(), is(equalTo(null)));
        assertThat(events.get(2).isAlert(), is(equalTo(true)));
    }

    // TODO: make more unit tests
}
