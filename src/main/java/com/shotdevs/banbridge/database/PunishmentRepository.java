/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.shotdevs.banbridge.database;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.database.DatabaseManager;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public class PunishmentRepository {
    private final DatabaseManager db;
    private final BanBridge plugin;

    public PunishmentRepository(BanBridge plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<Void> insert(Punishment punishment) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR IGNORE INTO punishments (id, type, target_username, target_uuid, target_discord_id, moderator_username, moderator_uuid, moderator_discord_id, reason, duration, duration_seconds, issued_at, expiry_at, server_name, source, active, evidence_url, discord_notified) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, punishment.getId());
                ps.setString(2, punishment.getType().name());
                ps.setString(3, punishment.getTargetUsername());
                ps.setString(4, punishment.hasTargetUuid() ? punishment.getTargetUuid().toString() : null);
                ps.setString(5, punishment.getTargetDiscordId());
                ps.setString(6, punishment.getModeratorUsername());
                ps.setString(7, punishment.hasModeratorUuid() ? punishment.getModeratorUuid().toString() : null);
                ps.setString(8, punishment.getModeratorDiscordId());
                ps.setString(9, punishment.getReason());
                ps.setString(10, punishment.getDuration());
                ps.setObject(11, punishment.getDurationSeconds());
                ps.setString(12, punishment.getIssuedAt() == null ? null : punishment.getIssuedAt().toString());
                ps.setString(13, punishment.getExpiryAt() == null ? null : punishment.getExpiryAt().toString());
                ps.setString(14, punishment.getServerName());
                ps.setString(15, punishment.getSource().name());
                ps.setBoolean(16, punishment.isActive());
                ps.setString(17, punishment.getEvidenceUrl());
                ps.setBoolean(18, punishment.isDiscordNotified());
                ps.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to insert punishment " + punishment.getId(), ex);
            }
        }, this.asyncExecutor());
    }

    public CompletableFuture<Optional<Punishment>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE id = ?";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return Optional.empty();
                    Optional<Punishment> optional = Optional.of(this.map(rs));
                    return optional;
                }
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to find punishment " + id, ex);
            }
            return Optional.empty();
        }, this.asyncExecutor());
    }

    public CompletableFuture<List<Punishment>> findByTarget(String targetUsername, String targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Punishment> list = new ArrayList<Punishment>();
            String sql = "SELECT * FROM punishments WHERE target_username = ? ORDER BY issued_at DESC";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, targetUsername);
                try (ResultSet rs = ps.executeQuery();){
                    while (rs.next()) {
                        list.add(this.map(rs));
                    }
                }
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to find history for " + targetUsername, ex);
            }
            return list;
        }, this.asyncExecutor());
    }

    public CompletableFuture<List<Punishment>> findActiveByTarget(String targetUsername) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Punishment> list = new ArrayList<Punishment>();
            String sql = "SELECT * FROM punishments WHERE target_username = ? AND active = 1 ORDER BY issued_at DESC";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, targetUsername);
                try (ResultSet rs = ps.executeQuery();){
                    while (rs.next()) {
                        list.add(this.map(rs));
                    }
                }
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to find active punishments for " + targetUsername, ex);
            }
            return list;
        }, this.asyncExecutor());
    }

    public CompletableFuture<List<Punishment>> findByModerator(String moderatorUuid) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Punishment> list = new ArrayList<Punishment>();
            String sql = "SELECT * FROM punishments WHERE moderator_uuid = ? ORDER BY issued_at DESC";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, moderatorUuid);
                try (ResultSet rs = ps.executeQuery();){
                    while (rs.next()) {
                        list.add(this.map(rs));
                    }
                }
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to find punishments by moderator " + moderatorUuid, ex);
            }
            return list;
        }, this.asyncExecutor());
    }

    public CompletableFuture<Integer> countWarnings(String targetUsername) {
        return this.countByTypeAndTarget(targetUsername, PunishmentType.WARNING.name());
    }

    public CompletableFuture<Integer> countByTypeAndTarget(String targetUsername, String typeName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM punishments WHERE target_username = ? AND type = ?";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, targetUsername);
                ps.setString(2, typeName);
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return 0;
                    Integer n = rs.getInt(1);
                    return n;
                }
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to count punishments", ex);
            }
            return 0;
        }, this.asyncExecutor());
    }

    public CompletableFuture<Void> updateDiscordNotified(String punishmentId, boolean notified) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE punishments SET discord_notified = ? WHERE id = ?";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setBoolean(1, notified);
                ps.setString(2, punishmentId);
                ps.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to update discord_notified for " + punishmentId, ex);
            }
        }, this.asyncExecutor());
    }

    public CompletableFuture<Void> revoke(String punishmentId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE punishments SET active = 0 WHERE id = ?";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, punishmentId);
                ps.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to revoke punishment " + punishmentId, ex);
            }
        }, this.asyncExecutor());
    }

    private Punishment map(ResultSet rs) throws SQLException {
        PunishmentType type = PunishmentType.valueOf(rs.getString("type"));
        PunishmentSource source = PunishmentSource.fromString(rs.getString("source"));
        UUID targetUuid = null;
        String tuuid = rs.getString("target_uuid");
        if (tuuid != null) {
            targetUuid = UUID.fromString(tuuid);
        }
        UUID moderatorUuid = null;
        String muuid = rs.getString("moderator_uuid");
        if (muuid != null) {
            moderatorUuid = UUID.fromString(muuid);
        }
        String issuedStr = rs.getString("issued_at");
        Instant issued = issuedStr == null ? null : Instant.parse(issuedStr);
        String expiryStr = rs.getString("expiry_at");
        Instant expiry = expiryStr == null ? null : Instant.parse(expiryStr);
        Long durationSeconds = rs.getObject("duration_seconds", Long.class);
        return new Punishment(rs.getString("id"), type, rs.getString("target_username"), targetUuid, rs.getString("target_discord_id"), rs.getString("moderator_username"), moderatorUuid, rs.getString("moderator_discord_id"), rs.getString("reason"), rs.getString("duration"), durationSeconds, issued, expiry, rs.getString("server_name"), source, rs.getBoolean("active"), rs.getString("evidence_url"), rs.getBoolean("discord_notified"));
    }

    private Executor asyncExecutor() {
        return task -> this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, task);
    }
}

