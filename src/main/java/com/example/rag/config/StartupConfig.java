package com.example.rag.config;

import com.example.rag.service.RagService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Runs PDF ingestion after the Spring context and PGVector store are ready. */
@Configuration
public class StartupConfig {

    @Bean
    public CommandLineRunner ingestCompanyPolicy(RagService ragService) {
        return arguments -> ragService.ingest();
    }
}
