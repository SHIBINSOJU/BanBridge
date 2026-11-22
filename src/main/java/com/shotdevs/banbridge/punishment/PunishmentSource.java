/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.punishment;

public enum PunishmentSource {
    NATIVE("Paper Native"),
    LITEBANS("LiteBans"),
    ADVANCEDBAN("AdvancedBan"),
    LIBERTYBANS("LibertyBans"),
    ESSENTIALSX("EssentialsX"),
    CMI("CMI"),
    DISCORD("Discord"),
    CONSOLE("Console"),
    UNKNOWN("Unknown");

    private final String label;

    private PunishmentSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public static PunishmentSource fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return PunishmentSource.valueOf(value.toUpperCase().replace("-", "").replace(" ", ""));
        }
        catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}

