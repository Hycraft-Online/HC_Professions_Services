package com.hcservices.database;

import com.hypixel.hytale.logger.HytaleLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-DB");

    private final HikariDataSource dataSource;

    public DatabaseManager(String jdbcUrl, String username, String password, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(1);
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectionTimeout(10000);
        config.setPoolName("HC_Services-DB-Pool");

        this.dataSource = new HikariDataSource(config);
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.at(Level.INFO).log("Database connection pool closed");
        }
    }

    private void initializeSchema() {
        String createListingsTable = """
            CREATE TABLE IF NOT EXISTS services_listings (
                id UUID PRIMARY KEY,
                provider_uuid UUID NOT NULL,
                provider_name VARCHAR(64),
                profession VARCHAR(32),
                title VARCHAR(128) NOT NULL,
                description VARCHAR(512),
                price INT DEFAULT 0,
                status VARCHAR(16) DEFAULT 'active',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createContractsTable = """
            CREATE TABLE IF NOT EXISTS services_contracts (
                id UUID PRIMARY KEY,
                listing_id UUID REFERENCES services_listings(id),
                provider_uuid UUID NOT NULL,
                provider_name VARCHAR(64),
                client_uuid UUID NOT NULL,
                client_name VARCHAR(64),
                status VARCHAR(16) DEFAULT 'requested',
                price INT NOT NULL,
                client_notes VARCHAR(512),
                provider_notified BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                completed_at TIMESTAMP
            )
            """;

        String createListingsIndexes = """
            CREATE INDEX IF NOT EXISTS idx_listings_provider ON services_listings(provider_uuid);
            CREATE INDEX IF NOT EXISTS idx_listings_status ON services_listings(status);
            CREATE INDEX IF NOT EXISTS idx_listings_profession ON services_listings(profession);
            """;

        String createContractsIndexes = """
            CREATE INDEX IF NOT EXISTS idx_contracts_provider ON services_contracts(provider_uuid);
            CREATE INDEX IF NOT EXISTS idx_contracts_client ON services_contracts(client_uuid);
            CREATE INDEX IF NOT EXISTS idx_contracts_status ON services_contracts(status);
            CREATE INDEX IF NOT EXISTS idx_contracts_listing ON services_contracts(listing_id);
            """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createListingsTable);
            stmt.execute(createContractsTable);
            // Execute index statements individually
            for (String indexSql : createListingsIndexes.split(";")) {
                String trimmed = indexSql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            for (String indexSql : createContractsIndexes.split(";")) {
                String trimmed = indexSql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            LOGGER.at(Level.INFO).log("Services database schema initialized");
        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize services schema: " + e.getMessage());
        }
    }
}
