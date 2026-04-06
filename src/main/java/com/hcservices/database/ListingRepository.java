package com.hcservices.database;

import com.hcservices.models.ServiceListing;
import com.hypixel.hytale.logger.HytaleLogger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ListingRepository {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Services-Listings");

    private final DatabaseManager db;

    public ListingRepository(DatabaseManager db) {
        this.db = db;
    }

    public void createListing(ServiceListing listing) {
        String sql = """
            INSERT INTO services_listings (id, provider_uuid, provider_name, profession, title, description, price, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, listing.getId());
            ps.setObject(2, listing.getProviderUuid());
            ps.setString(3, listing.getProviderName());
            ps.setString(4, listing.getProfession());
            ps.setString(5, listing.getTitle());
            ps.setString(6, listing.getDescription());
            ps.setInt(7, listing.getPrice());
            ps.setString(8, listing.getStatus());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to create listing: " + e.getMessage());
        }
    }

    public void updateListing(ServiceListing listing) {
        String sql = """
            UPDATE services_listings SET title = ?, description = ?, price = ?, status = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, listing.getTitle());
            ps.setString(2, listing.getDescription());
            ps.setInt(3, listing.getPrice());
            ps.setString(4, listing.getStatus());
            ps.setObject(5, listing.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to update listing: " + e.getMessage());
        }
    }

    public void deleteListing(UUID listingId) {
        // Delete associated contracts first, then the listing
        String deleteContracts = "DELETE FROM services_contracts WHERE listing_id = ?";
        String deleteListing = "DELETE FROM services_listings WHERE id = ?";
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(deleteContracts)) {
                ps.setObject(1, listingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(deleteListing)) {
                ps.setObject(1, listingId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to delete listing: " + e.getMessage());
        }
    }

    public ServiceListing getListingById(UUID id) {
        String sql = "SELECT * FROM services_listings WHERE id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapListing(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get listing: " + e.getMessage());
        }
        return null;
    }

    public List<ServiceListing> getActiveListings(int offset, int limit) {
        String sql = "SELECT * FROM services_listings WHERE status = 'active' ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        return queryListings(sql, limit, offset);
    }

    public List<ServiceListing> getActiveListingsByProfession(String profession, int offset, int limit) {
        String sql = "SELECT * FROM services_listings WHERE status = 'active' AND profession = ? ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        List<ServiceListing> results = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profession);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get listings by profession: " + e.getMessage());
        }
        return results;
    }

    public List<ServiceListing> getListingsByProvider(UUID providerUuid) {
        String sql = "SELECT * FROM services_listings WHERE provider_uuid = ? ORDER BY updated_at DESC";
        List<ServiceListing> results = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, providerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to get listings by provider: " + e.getMessage());
        }
        return results;
    }

    public int countActiveListingsByProvider(UUID providerUuid) {
        String sql = "SELECT COUNT(*) FROM services_listings WHERE provider_uuid = ? AND status = 'active'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, providerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to count listings: " + e.getMessage());
        }
        return 0;
    }

    public int countActiveListings() {
        String sql = "SELECT COUNT(*) FROM services_listings WHERE status = 'active'";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to count active listings: " + e.getMessage());
        }
        return 0;
    }

    public int countActiveListingsByProfession(String profession) {
        String sql = "SELECT COUNT(*) FROM services_listings WHERE status = 'active' AND profession = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profession);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to count listings by profession: " + e.getMessage());
        }
        return 0;
    }

    public void expireOldListings(int maxDays) {
        String sql = "UPDATE services_listings SET status = 'paused' WHERE status = 'active' AND updated_at < NOW() - make_interval(days => ?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, maxDays);
            int expired = ps.executeUpdate();
            if (expired > 0) {
                LOGGER.at(Level.INFO).log("Expired " + expired + " old service listings");
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to expire old listings: " + e.getMessage());
        }
    }

    private List<ServiceListing> queryListings(String sql, int limit, int offset) {
        List<ServiceListing> results = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapListing(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.at(Level.WARNING).log("Failed to query listings: " + e.getMessage());
        }
        return results;
    }

    private ServiceListing mapListing(ResultSet rs) throws SQLException {
        ServiceListing listing = new ServiceListing();
        listing.setId(rs.getObject("id", UUID.class));
        listing.setProviderUuid(rs.getObject("provider_uuid", UUID.class));
        listing.setProviderName(rs.getString("provider_name"));
        listing.setProfession(rs.getString("profession"));
        listing.setTitle(rs.getString("title"));
        listing.setDescription(rs.getString("description"));
        listing.setPrice(rs.getInt("price"));
        listing.setStatus(rs.getString("status"));
        listing.setCreatedAt(rs.getTimestamp("created_at"));
        listing.setUpdatedAt(rs.getTimestamp("updated_at"));
        return listing;
    }
}
