package com.test;

import com.test.db.Event;
import lombok.Data;

@Data
public class Message {
    private boolean shutdown = false;
    private Event event = null;

    public static Message shutdown() {
        Message message = new Message();
        message.setShutdown(true);
        return message;
    }

    public static Message event(Event event) {
        Message message = new Message();
        message.setEvent(event);
        return message;
    }
}
