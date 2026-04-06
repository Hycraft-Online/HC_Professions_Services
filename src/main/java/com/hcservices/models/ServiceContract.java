package com.hcservices.models;

import java.sql.Timestamp;
import java.util.UUID;

public class ServiceContract {

    private UUID id;
    private UUID listingId;
    private UUID providerUuid;
    private String providerName;
    private UUID clientUuid;
    private String clientName;
    private ContractStatus status;
    private int price;
    private String clientNotes;
    private boolean providerNotified;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp completedAt;

    public ServiceContract() {}

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getListingId() { return listingId; }
    public void setListingId(UUID listingId) { this.listingId = listingId; }

    public UUID getProviderUuid() { return providerUuid; }
    public void setProviderUuid(UUID providerUuid) { this.providerUuid = providerUuid; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public UUID getClientUuid() { return clientUuid; }
    public void setClientUuid(UUID clientUuid) { this.clientUuid = clientUuid; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public String getClientNotes() { return clientNotes; }
    public void setClientNotes(String clientNotes) { this.clientNotes = clientNotes; }

    public boolean isProviderNotified() { return providerNotified; }
    public void setProviderNotified(boolean providerNotified) { this.providerNotified = providerNotified; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
}
