package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.Team2ApplicationPayload;

import java.util.Optional;

/**
 * Outbound adapter for Chennai Team 2's Talent Acquisition Engine (REQ-JP-08).
 *
 * Decoupled as an interface so that:
 *  - Tests can mock it trivially with Mockito — no RestClient fluent-chain mocking needed.
 *  - A future Kafka-based implementation can be swapped in without touching any service.
 *
 * Returns the application ID that Team 2 assigns to the new record, or empty if the
 * call succeeded but Team 2 did not return an ID.
 */
public interface Team2Client {

    /**
     * Forward an application to Team 2.
     *
     * @param payload the candidate + job data to send
     * @return Team 2's assigned application ID, or empty if none was returned
     * @throws RuntimeException on HTTP error or connectivity failure (caller handles this)
     */
    Optional<String> send(Team2ApplicationPayload payload);
}
