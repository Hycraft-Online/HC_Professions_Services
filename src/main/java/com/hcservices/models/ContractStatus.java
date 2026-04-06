package com.hcservices.models;

import javax.annotation.Nullable;

public enum ContractStatus {
    REQUESTED("requested"),
    ACCEPTED("accepted"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    DECLINED("declined");

    private final String dbValue;

    ContractStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == DECLINED;
    }

    @Nullable
    public static ContractStatus fromString(String value) {
        if (value == null) return null;
        for (ContractStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
                return s;
            }
        }
        return null;
    }
}
