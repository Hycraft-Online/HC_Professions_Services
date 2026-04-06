package com.hcservices.models;

import java.sql.Timestamp;
import java.util.UUID;

public class ServiceListing {

    private UUID id;
    private UUID providerUuid;
    private String providerName;
    private String profession;
    private String title;
    private String description;
    private int price;
    private String status; // active / paused
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ServiceListing() {}

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProviderUuid() { return providerUuid; }
    public void setProviderUuid(UUID providerUuid) { this.providerUuid = providerUuid; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
