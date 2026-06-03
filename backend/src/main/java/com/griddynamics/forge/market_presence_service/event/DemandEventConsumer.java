package com.griddynamics.forge.market_presence_service.event;

import com.griddynamics.forge.market_presence_service.dto.DemandSnapshot;
import com.griddynamics.forge.market_presence_service.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandEventConsumer {

    private final JobPostingService jobPostingService;

    @KafkaListener(
        topics = "demand-opened",
        groupId = "${spring.kafka.consumer.group-id:market-presence-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDemandOpened(@Payload DemandSnapshot snapshot) {
        log.info("📥 Kafka event received: demand.opened demandId={} title='{}'",
            snapshot.demandId(), snapshot.title());
        try {
            jobPostingService.createFromDemand(snapshot);
            log.info("✅ Successfully created job posting from demand event");
        } catch (Exception e) {
            log.error("❌ Failed to create job posting from demand event: {}", e.getMessage());
        }
    }
}
