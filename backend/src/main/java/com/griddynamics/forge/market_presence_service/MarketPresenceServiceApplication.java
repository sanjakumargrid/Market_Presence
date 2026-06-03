	package com.griddynamics.forge.market_presence_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MarketPresenceServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketPresenceServiceApplication.class, args);
	}

}
