package com.jobportal.v1.enums;

public enum RoleEnum {
    ADMIN("ADMIN"),
    STAFF("STAFF"),
    AGENCY("AGENCY"),
    CANDIDATE("CANDIDATE");

    private final String role;

    RoleEnum(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public static RoleEnum fromString(String role) {
        for (RoleEnum r : RoleEnum.values()) {
            if (r.role.equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("No role found for value: " + role);
    }
}