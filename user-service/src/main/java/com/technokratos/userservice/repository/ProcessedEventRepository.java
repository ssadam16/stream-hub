package com.technokratos.userservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT =
            "INSERT INTO processed_events (event_id, processed_at) " +
                    "VALUES (?, now()) ON CONFLICT (event_id) DO NOTHING";

    public boolean tryMarkProcessed(UUID eventId) {
        int rows = jdbcTemplate.update(INSERT, eventId);
        return rows == 1; // true = вставили впервые, false = уже был
    }
}