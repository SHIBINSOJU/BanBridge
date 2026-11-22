/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.shotdevs.banbridge.database;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.DatabaseManager;
import com.shotdevs.banbridge.discord.DiscordNotifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public class NotificationQueue {
    private final BanBridge plugin;
    private final DatabaseManager db;
    private final DiscordNotifier notifier;
    private final ConfigManager config;
    private final ConcurrentLinkedQueue<String> memoryQueue = new ConcurrentLinkedQueue();
    private boolean processing = false;

    public NotificationQueue(BanBridge plugin, DiscordNotifier notifier) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
        this.notifier = notifier;
        this.config = plugin.getConfigManager();
    }

    public void enqueue(String payloadKey) {
        this.memoryQueue.add(payloadKey);
        this.persist(payloadKey);
        if (this.config.isAsyncProcessing() && !this.processing) {
            this.process();
        }
    }

    private void persist(String payloadKey) {
        CompletableFuture.runAsync(() -> {
            String sql = "INSERT OR IGNORE INTO notification_queue (payload_key, created_at, attempts) VALUES (?, ?, 0)";
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);){
                ps.setString(1, payloadKey);
                ps.setObject(2, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to persist notification to queue", ex);
            }
        }, task -> this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, task));
    }

    public void process() {
        if (this.processing) {
            return;
        }
        this.processing = true;
        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, () -> {
            try {
                this.drainQueue();
            }
            catch (Exception ex) {
                this.plugin.getLogger().log(Level.WARNING, "Notification queue processing error", ex);
            }
            if (this.memoryQueue.isEmpty() && this.pendingDbCount() == 0) {
                this.processing = false;
            }
        }, 0L, 20L);
    }

    private void drainQueue() {
        String key;
        while ((key = this.memoryQueue.poll()) != null) {
            boolean sent = this.notifier.sendQueued(key);
            if (!sent) {
                this.memoryQueue.add(key);
                break;
            }
            this.markDelivered(key);
        }
        this.processDbQueue();
    }

    private void processDbQueue() {
        ArrayList<String> due = null;
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT payload_key FROM notification_queue WHERE delivered = 0 AND attempts < ? ORDER BY created_at ASC LIMIT 50");){
            ps.setInt(1, this.config.getMaxRetries());
            try (ResultSet rs = ps.executeQuery();){
                due = new ArrayList<String>();
                while (rs.next()) {
                    due.add(rs.getString("payload_key"));
                }
            }
        }
        catch (SQLException ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to read notification queue", ex);
        }
        if (due == null) {
            return;
        }
        for (String key : due) {
            boolean sent = this.notifier.sendQueued(key);
            if (sent) {
                this.markDelivered(key);
                continue;
            }
            this.incrementAttempts(key);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private int pendingDbCount() {
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notification_queue WHERE delivered = 0 AND attempts < ?");){
            ps.setInt(1, this.config.getMaxRetries());
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return 0;
                int n = rs.getInt(1);
                return n;
            }
        }
        catch (SQLException sQLException) {
            // empty catch block
        }
        return 0;
    }

    private void markDelivered(String key) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE notification_queue SET delivered = 1 WHERE payload_key = ?");){
                ps.setString(1, key);
                ps.executeUpdate();
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
        }, task -> this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, task));
    }

    private void incrementAttempts(String key) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = this.db.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE notification_queue SET attempts = attempts + 1 WHERE payload_key = ?");){
                ps.setString(1, key);
                ps.executeUpdate();
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
        }, task -> this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, task));
    }

    public int memorySize() {
        return this.memoryQueue.size();
    }
}

