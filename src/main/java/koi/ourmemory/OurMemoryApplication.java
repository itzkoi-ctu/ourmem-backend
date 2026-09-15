package koi.ourmemory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OurMemoryApplication {

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        loadEnv();
        runFlywayMigration();
        SpringApplication.run(OurMemoryApplication.class, args);
    }


    private static void loadEnv() {
        try {
            java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
            if (java.nio.file.Files.exists(envPath)) {
                System.out.println(">>> LOAD ENV: Found .env file at " + envPath.toAbsolutePath());
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envPath);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        System.setProperty(key, value);
                        System.out.println(">>> LOAD ENV: Set system property " + key);
                    }
                }
            } else {
                System.err.println(">>> LOAD ENV: .env file NOT found at " + envPath.toAbsolutePath());
            }
        } catch (java.io.IOException e) {
            System.err.println(">>> LOAD ENV: Failed to load .env file: " + e.getMessage());
        }
    }

    private static void runFlywayMigration() {
        String url = System.getProperty("SUPABASE_DB_DIRECT_URL");
        String user = System.getProperty("SUPABASE_DB_DIRECT_USERNAME");
        if (user == null || user.trim().isEmpty()) {
            user = "postgres";
        }
        String password = System.getProperty("SUPABASE_DB_PASSWORD");

        if (url == null || password == null) {
            System.err.println(">>> MANUAL FLYWAY: Database URL or password not set in .env. Skipping manual Flyway migration.");
            return;
        }

        try {
            System.out.println(">>> MANUAL FLYWAY: Starting migration using " + url);
            org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                    .dataSource(url, user, password)
                    .locations("classpath:db/migration", "filesystem:src/main/resources/db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load();
            System.out.println(">>> MANUAL FLYWAY: Repairing schema history...");
            flyway.repair();
            org.flywaydb.core.api.output.MigrateResult result = flyway.migrate();
            System.out.println(">>> MANUAL FLYWAY: Migration result success = " + result.success);
        } catch (Exception e) {
            System.err.println(">>> MANUAL FLYWAY: Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
