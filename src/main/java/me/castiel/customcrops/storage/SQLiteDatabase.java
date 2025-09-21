package me.castiel.customcrops.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class SQLiteDatabase {

    private HikariDataSource dataSource;

    public SQLiteDatabase(String databasePath) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("CustomCropsPool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
    }

    public CompletableFuture<Void> initializeDatabase() {
        return CompletableFuture.runAsync(() -> {

            try (Connection connection = getConnection()) {
                String createCropsTableSQL = """
                    CREATE TABLE IF NOT EXISTS crops (
                        worldName TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        material TEXT NOT NULL,
                        plantedAt INTEGER NOT NULL,
                        PRIMARY KEY (worldName, x, y, z)
                    );
                """;
                String createBalancesTableSQL = """
                    CREATE TABLE IF NOT EXISTS balances (
                        playerUUID TEXT NOT NULL,
                        cubes INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (playerUUID)
                    );
                """;
                // Add this to the `initializeDatabase` method in `SQLiteDatabase.java`
                String createPlayerHighestItemTableSQL = """
                    CREATE TABLE IF NOT EXISTS player_highest_item (
                        playerUUID TEXT NOT NULL,
                        highestRequirement INTEGER NOT NULL,
                        PRIMARY KEY (playerUUID)
                    );
                """;
                connection.createStatement().execute(createCropsTableSQL);
                connection.createStatement().execute(createBalancesTableSQL);
                connection.createStatement().execute(createPlayerHighestItemTableSQL);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to initialize database", e);
            }
        });
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}