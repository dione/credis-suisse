package com.test.db;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oid;

    @Column(nullable = false)
    private String id;
    @Column(nullable = false)
    private Long duration;
    @Column
    private String type;
    @Column
    private String host;
    @Column(nullable = false)
    private boolean alert = false;
}
