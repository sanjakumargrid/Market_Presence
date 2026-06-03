package com.griddynamics.forge.market_presence_service.service;

import com.griddynamics.forge.market_presence_service.dto.Team2ApplicationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP implementation of Team2Client.
 *
 * Posts to {@code app.team2.api-base-url}/applications/intake when configured.
 * Uses Spring's RestClient (available since Spring Framework 6.1 / Spring Boot 3.2+).
 *
 * This class is only registered as a Spring bean for wiring purposes — the
 * liveness of the URL is checked at call-time, not at startup.
 */
@Component
public class HttpTeam2Client implements Team2Client {

    private static final Logger log = LoggerFactory.getLogger(HttpTeam2Client.class);

    private final RestClient restClient;
    private final String     apiBaseUrl;

    public HttpTeam2Client(
            @Value("${app.team2.api-base-url:}") String apiBaseUrl,
            @Value("${app.team2.timeout-seconds:10}") int timeoutSeconds) {

        this.apiBaseUrl = apiBaseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        if (StringUtils.hasText(apiBaseUrl)) {
            log.info("[TEAM2] Client configured — base URL: {}", apiBaseUrl);
        }
    }

    @Override
    public Optional<String> send(Team2ApplicationPayload payload) {
        if (!StringUtils.hasText(apiBaseUrl)) {
            throw new IllegalStateException("Team 2 URL is not configured — caller should check before calling send()");
        }

        String endpoint = apiBaseUrl.stripTrailing() + "/applications/intake";

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("id")) {
            return Optional.of(String.valueOf(response.get("id")));
        }
        return Optional.empty();
    }
}
