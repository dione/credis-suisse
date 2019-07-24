package com.test.log;

import lombok.Data;

@Data
public class LogLine {
    private String id;
    private String state;
    private Long timestamp;
    private String type;
    private String host;
}
