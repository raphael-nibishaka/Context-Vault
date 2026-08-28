package database;

import models.DebugEntry;
import repository.DebugRepository;

import java.util.List;

public class DebugSampleDataSeeder {
    private final DebugRepository debugRepository;

    public DebugSampleDataSeeder(DebugRepository debugRepository) {
        this.debugRepository = debugRepository;
    }

    public void seedIfEmpty() {
        if (!debugRepository.findAll().isEmpty()) {
            return;
        }

        List<DebugEntry> samples = List.of(
                DebugEntry.newEntry(
                        "MongoServerSelectionError: connect ECONNREFUSED 127.0.0.1:27017",
                        """
                                MongoServerSelectionError: connect ECONNREFUSED 127.0.0.1:27017
                                    at Topology.selectServer (/node_modules/mongodb/lib/sdam/topology.js:303:30)
                                    at async connect (/src/database.ts:18:5)
                                """.trim(),
                        "MongoServerSelectionError",
                        "AgriSense",
                        "C:\\Projects\\AgriSense",
                        "database.ts",
                        "Docker MongoDB container wasn't running.",
                        "docker compose up mongodb",
                        "Backend startup blocked while implementing JWT authentication.",
                        "mongodb, docker, database"
                ),
                DebugEntry.newEntry(
                        "MongoNetworkError: connection refused",
                        """
                                MongoNetworkError: failed to connect to server localhost:27017
                                    at Socket.<anonymous> (/node_modules/mongodb/lib/cmap/connect.js:286:44)
                                """.trim(),
                        "MongoNetworkError",
                        "AgriSense",
                        "C:\\Projects\\AgriSense",
                        "database.ts",
                        "Restart the MongoDB container after a machine reboot.",
                        "docker compose up -d mongodb",
                        "Seen during local API smoke tests.",
                        "mongodb, connection"
                ),
                DebugEntry.newEntry(
                        "401 Unauthorized: invalid refresh token",
                        """
                                Error: 401 Unauthorized
                                    at validateRefreshToken (/src/middleware/auth.ts:42:11)
                                """.trim(),
                        "UnauthorizedError",
                        "AgriSense",
                        "C:\\Projects\\AgriSense",
                        "src/middleware/auth.ts",
                        "Refresh token middleware rejected expired tokens without a clear rotation path.",
                        "npm run test:auth",
                        "JWT authentication workstream.",
                        "auth, jwt, middleware"
                )
        );

        samples.forEach(debugRepository::save);
    }
}
