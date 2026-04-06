package com.hcservices.database;

import com.hcservices.models.ContractStatus;
import com.hcservices.models.ServiceContract;
import com.hypixel.hytale.logger.HytaleLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ContractRepository {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Contracts");

    private final DatabaseManager db;

    public ContractRepository(DatabaseManager db) {
        this.db = db;
    }

    public void createContract(ServiceContract contract) {
        String sql = """
            INSERT INTO services_contracts (id, listing_id, provider_uuid, provider_name, client_uuid, client_name, status, price, client_notes, provider_notified, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, contract.getId());
            ps.setObject(2, contract.getListingId());
            ps.setObject(3, contract.getProviderUuid());
            ps.setString(4, contract.getProviderName());
            ps.setObject(5, contract.getClientUuid());
            ps.setString(6, contract.getClientName());
            ps.setString(7, contract.getStatus().getDbValue());
            ps.setInt(8, contract.getPrice());
            ps.setString(9, contract.getClientNotes());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to create contract: " + e.getMessage());
        }
    }

    public void updateStatus(UUID contractId, ContractStatus newStatus) {
        String sql;
        if (newStatus == ContractStatus.COMPLETED) {
            sql = "UPDATE services_contracts SET status = ?, updated_at = CURRENT_TIMESTAMP, completed_at = CURRENT_TIMESTAMP WHERE id = ?";
        } else {
            sql = "UPDATE services_contracts SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        }
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.getDbValue());
            ps.setObject(2, contractId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to update contract status: " + e.getMessage());
        }
    }

    public ServiceContract getContractById(UUID id) {
        String sql = "SELECT * FROM services_contracts WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapContract(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get contract: " + e.getMessage());
        }
        return null;
    }

    public List<ServiceContract> getContractsByProvider(UUID providerUuid) {
        String sql = "SELECT * FROM services_contracts WHERE provider_uuid = ? ORDER BY updated_at DESC";
        return queryContracts(sql, providerUuid);
    }

    public List<ServiceContract> getContractsByClient(UUID clientUuid) {
        String sql = "SELECT * FROM services_contracts WHERE client_uuid = ? ORDER BY updated_at DESC";
        return queryContracts(sql, clientUuid);
    }

    public int countActiveContractsByProvider(UUID providerUuid) {
        String sql = "SELECT COUNT(*) FROM services_contracts WHERE provider_uuid = ? AND status NOT IN ('completed', 'cancelled', 'declined')";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, providerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to count active contracts: " + e.getMessage());
        }
        return 0;
    }

    public List<ServiceContract> getUnnotifiedContracts(UUID providerUuid) {
        String sql = "SELECT * FROM services_contracts WHERE provider_uuid = ? AND provider_notified = false AND status = 'requested'";
        return queryContracts(sql, providerUuid);
    }

    public void markNotified(UUID contractId) {
        String sql = "UPDATE services_contracts SET provider_notified = true WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, contractId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to mark contract notified: " + e.getMessage());
        }
    }

    private List<ServiceContract> queryContracts(String sql, UUID uuid) {
        List<ServiceContract> results = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapContract(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to query contracts: " + e.getMessage());
        }
        return results;
    }

    private ServiceContract mapContract(ResultSet rs) throws SQLException {
        ServiceContract contract = new ServiceContract();
        contract.setId(rs.getObject("id", UUID.class));
        contract.setListingId(rs.getObject("listing_id", UUID.class));
        contract.setProviderUuid(rs.getObject("provider_uuid", UUID.class));
        contract.setProviderName(rs.getString("provider_name"));
        contract.setClientUuid(rs.getObject("client_uuid", UUID.class));
        contract.setClientName(rs.getString("client_name"));
        contract.setStatus(ContractStatus.fromString(rs.getString("status")));
        contract.setPrice(rs.getInt("price"));
        contract.setClientNotes(rs.getString("client_notes"));
        contract.setProviderNotified(rs.getBoolean("provider_notified"));
        contract.setCreatedAt(rs.getTimestamp("created_at"));
        contract.setUpdatedAt(rs.getTimestamp("updated_at"));
        contract.setCompletedAt(rs.getTimestamp("completed_at"));
        return contract;
    }
}
