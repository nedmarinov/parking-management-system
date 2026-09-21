package com.example.parking.integration;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Rebuilds the integration database from an empty schema on context startup,
 * after confirming it is a designated test database.
 */
@TestConfiguration
public class TestDatabaseConfig {

    @Bean
    FlywayMigrationStrategy cleanMigrateTestDatabase() {
        return flyway -> {
            requireTestDatabase(flyway.getConfiguration().getDataSource());
            flyway.clean();
            flyway.migrate();
        };
    }

    private static void requireTestDatabase(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String database = connection.getCatalog();
            if (database == null || !database.endsWith("_test")) {
                throw new IllegalStateException(
                        "Refusing to run integration tests against database '" + database
                                + "'; TEST_DATABASE_URL must point to a database whose name ends in _test");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect to the integration test database", e);
        }
    }
}
