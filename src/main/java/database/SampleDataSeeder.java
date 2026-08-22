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
                        "AgriSense Authentication",
                        "AgriSense",
                        "C:\\Projects\\AgriSense",
                        "C:\\Projects\\AgriSense",
                        "feature/authentication",
                        """
                                src/auth/AuthService.java
                                src/auth/JwtService.java
                                src/middleware/AuthMiddleware.java
                                """.trim(),
                        "Fix refresh token validation.",
                        """
                                npm run dev
                                docker compose up
                                """.trim(),
                        "auth, backend",
                        """
                                http://localhost:3000
                                https://github.com/example/agrisense
                                """.trim()
                ),
                ContextEntry.newEntry(
                        "Payments Platform",
                        "FintechSuite",
                        "C:\\Projects\\FintechSuite\\payments-service",
                        "C:\\Projects\\FintechSuite\\payments-service",
                        "bugfix/reconcile-timeout",
                        """
                                src/main/java/com/fintech/ReconcileJob.java
                                src/test/java/com/fintech/ReconcileJobTest.java
                                """.trim(),
                        "Investigate timeout spikes in nightly reconciliation.",
                        """
                                mvn clean test
                                mvn spring-boot:run
                                """.trim(),
                        "payments, java",
                        "http://localhost:8080/actuator/health"
                ),
                ContextEntry.newEntry(
                        "Mobile API Gateway",
                        "Gateway API",
                        "D:\\Work\\gateway-api",
                        "D:\\Work\\gateway-api",
                        "release/v1.4.2",
                        """
                                src/routes/index.ts
                                src/config/env.ts
                                """.trim(),
                        "Prepare smoke test checklist and compare staging logs.",
                        """
                                docker compose up -d
                                ./gradlew test
                                """.trim(),
                        "gateway, release",
                        "http://localhost:4000/docs"
                )
        );

        samples.forEach(contextRepository::save);
    }
}
