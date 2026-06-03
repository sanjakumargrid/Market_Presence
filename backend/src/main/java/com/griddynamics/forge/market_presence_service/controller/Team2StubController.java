package com.griddynamics.forge.market_presence_service.controller;

import com.griddynamics.forge.market_presence_service.dto.Team2ApplicationPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Demo stub for Chennai Team 2's Talent Acquisition Engine (REQ-JP-08).
 *
 * This controller lives at the same base URL as the market-presence-service so the
 * handoff loop works end-to-end without Team 2 actually running.
 *
 * Wire-up: application.yml sets app.team2.api-base-url to http://localhost:8086/api/stub/team2.
 * HttpTeam2Client POSTs to {base-url}/applications/intake — which lands here.
 *
 * When Team 2's real URL is known, replace app.team2.api-base-url and this stub
 * becomes unreachable (but harmless to leave registered).
 */
@RestController
@RequestMapping("/api/stub/team2")
@Tag(name = "Demo Stub — Team 2",
     description = "Local stand-in for Chennai Team 2's intake endpoint (REQ-JP-08 demo mode). " +
                   "Replace app.team2.api-base-url with the real URL to disable.")
public class Team2StubController {

    private static final Logger log = LoggerFactory.getLogger(Team2StubController.class);

    @PostMapping("/applications/intake")
    @Operation(
            summary = "Stub: accept a candidate application handoff from Team 3",
            description = "Mimics Team 2's POST /applications/intake. Returns a generated demo application ID. " +
                          "Set app.team2.api-base-url to Team 2's real URL to bypass this stub in production.")
    public Map<String, Object> intake(@RequestBody Team2ApplicationPayload payload) {
        String demoId = "T2-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[TEAM2-STUB] Handoff received  candidate={} job='{}' source={} → assigned {}",
                payload.email(), payload.jobTitle(), payload.source(), demoId);

        return Map.of(
                "id",          demoId,
                "status",      "RECEIVED",
                "source",      payload.source(),
                "receivedAt",  Instant.now().toString(),
                "note",        "Demo stub — replace app.team2.api-base-url with real Team 2 URL for production"
        );
    }
}
