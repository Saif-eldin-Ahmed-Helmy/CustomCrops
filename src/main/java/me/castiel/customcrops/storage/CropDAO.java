package me.castiel.customcrops.storage;

import me.castiel.customcrops.crops.CropData;
import me.castiel.customcrops.crops.CropFactory;
import org.bukkit.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class CropDAO {

    private final SQLiteDatabase database;

    public CropDAO(SQLiteDatabase database) {
        this.database = database;
    }

    public CompletableFuture<Void> saveCrops(List<CropData> cropDataList) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO crops (worldName, x, y, z, material, plantedAt)
                VALUES (?, ?, ?, ?, ?, ?);
            """;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                for (CropData cropData : cropDataList) {
                    statement.setString(1, cropData.getWorldName());
                    statement.setInt(2, cropData.getX());
                    statement.setInt(3, cropData.getY());
                    statement.setInt(4, cropData.getZ());
                    statement.setString(5, cropData.getMaterial().name());
                    statement.setLong(6, cropData.getPlantedAt());
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save crop data", e);
            }
        });
    }

    public void saveCropsSync(List<CropData> cropDataList) {
        String sql = """
                INSERT OR REPLACE INTO crops (worldName, x, y, z, material, plantedAt)
                VALUES (?, ?, ?, ?, ?, ?);
            """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (CropData cropData : cropDataList) {
                statement.setString(1, cropData.getWorldName());
                statement.setInt(2, cropData.getX());
                statement.setInt(3, cropData.getY());
                statement.setInt(4, cropData.getZ());
                statement.setString(5, cropData.getMaterial().name());
                statement.setLong(6, cropData.getPlantedAt());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save crop data", e);
        }
    }

    public CompletableFuture<Map<String, Long>> loadAllBalances() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Long> balances = new HashMap<>();
            String sql = "SELECT * FROM balances;";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    balances.put(resultSet.getString("playerUUID"), resultSet.getLong("cubes"));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load balances", e);
            }
            return balances;
        });
    }

    public CompletableFuture<Set<CropData>> loadAllCrops() {
        return CompletableFuture.supplyAsync(() -> {
            Set<CropData> crops = new HashSet<>();
            String sql = "SELECT * FROM crops;";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    crops.add(CropFactory.fromSaved(
                            resultSet.getString("worldName"),
                            resultSet.getInt("x"),
                            resultSet.getInt("y"),
                            resultSet.getInt("z"),
                            Material.valueOf(resultSet.getString("material")),
                            resultSet.getLong("plantedAt")
                    ));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load crops", e);
            }
            return crops;
        });
    }

    public CompletableFuture<Void> setBalance(String playerUUID, int cubes) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO balances (playerUUID, cubes)
                VALUES (?, ?);
            """;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUUID);
                statement.setInt(2, cubes);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set balance", e);
            }
        });
    }

    public CompletableFuture<Void> addBalance(String playerUUID, int cubesToAdd) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                UPDATE balances
                SET cubes = cubes + ?
                WHERE playerUUID = ?;
            """;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, cubesToAdd);
                statement.setString(2, playerUUID);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add balance", e);
            }
        });
    }

    public CompletableFuture<Void> setBalances(HashMap<String, Long> changedBalances) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO balances (playerUUID, cubes)
                VALUES (?, ?);
            """;
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                for (Map.Entry<String, Long> entry : changedBalances.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setLong(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to set balances", e);
            }
        });
    }

    public void setBalancesSync(HashMap<String, Long> changedBalances) {
        String sql = """
                INSERT OR REPLACE INTO balances (playerUUID, cubes)
                VALUES (?, ?);
            """;
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Long> entry : changedBalances.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setLong(2, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set balances", e);
        }
    }
}