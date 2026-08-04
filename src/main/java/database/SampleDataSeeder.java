package database;

import models.ContextEntry;
import repository.ContextRepository;

import java.util.List;

public class SampleDataSeeder {
    private final ContextRepository contextRepository;

    public SampleDataSeeder(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public void seedIfEmpty() {
        if (!contextRepository.findAll().isEmpty()) {
            return;
        }

        List<ContextEntry> samples = List.of(
                ContextEntry.newEntry(
                        "AgriSense Backend",
                        "C:\\Projects\\AgriSense\\backend",
                        "feature/auth",
                        "Need to fix JWT refresh token handling before QA review.",
                        "npm install%npm run dev%ndocker compose up".formatted()
                ),
                ContextEntry.newEntry(
                        "Payments Platform",
                        "C:\\Projects\\FintechSuite\\payments-service",
                        "bugfix/reconcile-timeout",
                        "Investigate timeout spikes in nightly reconciliation.",
                        "mvn clean test%nmvn spring-boot:run".formatted()
                ),
                ContextEntry.newEntry(
                        "Mobile API Gateway",
                        "D:\\Work\\gateway-api",
                        "release/v1.4.2",
                        "Prepare smoke test checklist and compare staging logs.",
                        "docker compose up -d%n./gradlew test".formatted()
                )
        );

        samples.forEach(contextRepository::save);
    }
}
