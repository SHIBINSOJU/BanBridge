/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.punishment;

import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Punishment {
    private final String id;
    private final PunishmentType type;
    private final String targetUsername;
    private final UUID targetUuid;
    private final String targetDiscordId;
    private final String moderatorUsername;
    private final UUID moderatorUuid;
    private final String moderatorDiscordId;
    private final String reason;
    private final String duration;
    private final Long durationSeconds;
    private final Instant issuedAt;
    private final Instant expiryAt;
    private final String serverName;
    private final PunishmentSource source;
    private final boolean active;
    private final String evidenceUrl;
    private final boolean discordNotified;

    public Punishment(String id, PunishmentType type, String targetUsername, UUID targetUuid, String targetDiscordId, String moderatorUsername, UUID moderatorUuid, String moderatorDiscordId, String reason, String duration, Long durationSeconds, Instant issuedAt, Instant expiryAt, String serverName, PunishmentSource source, boolean active, String evidenceUrl, boolean discordNotified) {
        this.id = id == null ? this.generateId() : id;
        this.type = type == null ? PunishmentType.WARNING : type;
        this.targetUsername = targetUsername == null ? "UNKNOWN" : targetUsername;
        this.targetUuid = targetUuid;
        this.targetDiscordId = targetDiscordId;
        this.moderatorUsername = moderatorUsername == null ? "UNKNOWN" : moderatorUsername;
        this.moderatorUuid = moderatorUuid;
        this.moderatorDiscordId = moderatorDiscordId;
        this.reason = reason == null || reason.isBlank() ? "No reason provided" : reason;
        this.duration = duration == null || duration.isBlank() ? "UNKNOWN" : duration;
        this.durationSeconds = durationSeconds;
        this.issuedAt = issuedAt == null ? Instant.now() : issuedAt;
        this.expiryAt = expiryAt;
        this.serverName = serverName == null ? "UNKNOWN" : serverName;
        this.source = source == null ? PunishmentSource.UNKNOWN : source;
        this.active = active;
        this.evidenceUrl = evidenceUrl;
        this.discordNotified = discordNotified;
    }

    private String generateId() {
        return "bb-" + UUID.randomUUID().toString().substring(0, 12);
    }

    public String getId() {
        return this.id;
    }

    public PunishmentType getType() {
        return this.type;
    }

    public String getTargetUsername() {
        return this.targetUsername;
    }

    public UUID getTargetUuid() {
        return this.targetUuid;
    }

    public String getTargetDiscordId() {
        return this.targetDiscordId;
    }

    public String getModeratorUsername() {
        return this.moderatorUsername;
    }

    public UUID getModeratorUuid() {
        return this.moderatorUuid;
    }

    public String getModeratorDiscordId() {
        return this.moderatorDiscordId;
    }

    public String getReason() {
        return this.reason;
    }

    public String getDuration() {
        return this.duration;
    }

    public Long getDurationSeconds() {
        return this.durationSeconds;
    }

    public Instant getIssuedAt() {
        return this.issuedAt;
    }

    public Instant getExpiryAt() {
        return this.expiryAt;
    }

    public String getServerName() {
        return this.serverName;
    }

    public PunishmentSource getSource() {
        return this.source;
    }

    public boolean isActive() {
        return this.active;
    }

    public String getEvidenceUrl() {
        return this.evidenceUrl;
    }

    public boolean isDiscordNotified() {
        return this.discordNotified;
    }

    public boolean isExpired() {
        return this.expiryAt != null && Instant.now().isAfter(this.expiryAt);
    }

    public boolean hasTargetUuid() {
        return this.targetUuid != null;
    }

    public boolean hasModeratorUuid() {
        return this.moderatorUuid != null;
    }

    public boolean hasTargetDiscordId() {
        return this.targetDiscordId != null && !this.targetDiscordId.isBlank();
    }

    public boolean hasModeratorDiscordId() {
        return this.moderatorDiscordId != null && !this.moderatorDiscordId.isBlank();
    }

    public boolean hasEvidence() {
        return this.evidenceUrl != null && !this.evidenceUrl.isBlank();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Punishment)) {
            return false;
        }
        Punishment that = (Punishment)o;
        return Objects.equals(this.id, that.id);
    }

    public int hashCode() {
        return Objects.hash(this.id);
    }

    public String toString() {
        return "Punishment{id='" + this.id + "', type=" + String.valueOf((Object)this.type) + ", target=" + this.targetUsername + ", moderator=" + this.moderatorUsername + ", active=" + this.active + "}";
    }
}

