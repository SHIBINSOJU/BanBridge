/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.punishment;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.NotificationQueue;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.discord.DiscordNotifier;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentType;
import com.shotdevs.banbridge.statistics.StatisticsManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PunishmentManager {
    private final BanBridge plugin;
    private final ConfigManager config;
    private final PunishmentRepository repository;
    private NotificationQueue queue;
    private DiscordNotifier notifier;
    private final StatisticsManager statistics;
    private final Map<String, Long> recentPunishments = new HashMap<String, Long>();
    private static final long DEDUP_WINDOW_MS = 600000L;

    public PunishmentManager(BanBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.repository = plugin.getDatabaseManager() != null ? new PunishmentRepository(plugin) : null;
        this.statistics = new StatisticsManager(plugin);
    }

    public void wire(DiscordNotifier notifier, NotificationQueue queue) {
        this.notifier = notifier;
        this.queue = queue;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean record(Punishment punishment) {
        if (punishment == null) {
            return false;
        }
        String id = punishment.getId();
        String dedupKey = String.valueOf((Object)punishment.getType()) + "|" + punishment.getTargetUsername() + "|" + punishment.getModeratorUsername() + "|" + punishment.getReason();
        long now = System.currentTimeMillis();
        Map<String, Long> map = this.recentPunishments;
        synchronized (map) {
            Long last = this.recentPunishments.get(dedupKey);
            if (last != null && now - last < 600000L) {
                return false;
            }
            this.recentPunishments.put(dedupKey, now);
        }
        this.evictOld();
        if (this.repository != null) {
            this.repository.insert(punishment).exceptionally(ex -> {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to persist punishment " + id, (Throwable)ex);
                return null;
            });
        }
        this.statistics.recordAction(punishment);
        if (this.config.isMinecraftToDiscord() && !punishment.isDiscordNotified() && this.shouldNotify(punishment)) {
            if (this.config.isAsyncProcessing()) {
                if (this.queue != null) {
                    this.queue.enqueue(id);
                } else if (this.notifier != null) {
                    this.notifier.notify(punishment);
                }
            } else if (this.notifier != null) {
                this.notifier.notify(punishment);
            }
        }
        this.plugin.getLogger().info("Recorded punishment: " + String.valueOf((Object)punishment.getType()) + " on " + punishment.getTargetUsername() + " by " + punishment.getModeratorUsername());
        return true;
    }

    private void evictOld() {
        long cutoff = System.currentTimeMillis() - 600000L;
        this.recentPunishments.entrySet().removeIf(e -> (Long)e.getValue() < cutoff);
    }

    private boolean shouldNotify(Punishment p) {
        return switch (p.getType()) {
            case PunishmentType.BAN -> this.config.isNotifyBans();
            case PunishmentType.TEMP_BAN -> this.config.isNotifyBans();
            case PunishmentType.IP_BAN -> this.config.isNotifyIpBans();
            case PunishmentType.TEMP_IP_BAN -> this.config.isNotifyIpBans();
            case PunishmentType.UNBAN -> this.config.isNotifyUnbans();
            case PunishmentType.IP_UNBAN -> this.config.isNotifyUnbans();
            case PunishmentType.PERM_MUTE, PunishmentType.TEMP_MUTE -> this.config.isNotifyMutes();
            case PunishmentType.UNMUTE -> this.config.isNotifyUnmutes();
            case PunishmentType.WARNING -> this.config.isNotifyWarnings();
            case PunishmentType.KICK -> this.config.isNotifyKicks();
            case PunishmentType.FREEZE -> this.config.isNotifyFreezes();
            case PunishmentType.UNFREEZE -> this.config.isNotifyUnfreezes();
            case PunishmentType.JAIL -> this.config.isNotifyJails();
            case PunishmentType.UNJAIL -> this.config.isNotifyUnjails();
            case PunishmentType.DISCORD_BAN -> this.config.isNotifyDiscordBans();
            case PunishmentType.DISCORD_TEMP_BAN -> this.config.isNotifyDiscordBans();
            case PunishmentType.DISCORD_UNBAN -> this.config.isNotifyDiscordUnbans();
            case PunishmentType.DISCORD_KICK -> this.config.isNotifyDiscordKicks();
            case PunishmentType.DISCORD_TIMEOUT -> this.config.isNotifyDiscordTimeouts();
            case PunishmentType.DISCORD_TIMEOUT_REMOVAL -> this.config.isNotifyDiscordTimeoutRemovals();
            default -> true;
        };
    }

    public CompletableFuture<Optional<Punishment>> getPunishment(String id) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return this.repository.findById(id);
    }

    public CompletableFuture<List<Punishment>> getHistory(String targetUsername) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return this.repository.findByTarget(targetUsername, null);
    }

    public CompletableFuture<List<Punishment>> getActive(String targetUsername) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return this.repository.findActiveByTarget(targetUsername);
    }

    public CompletableFuture<List<Punishment>> getByModerator(String moderatorUuid) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return this.repository.findByModerator(moderatorUuid);
    }

    public CompletableFuture<Integer> countWarnings(String targetUsername) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(0);
        }
        return this.repository.countByTypeAndTarget(targetUsername, PunishmentType.WARNING.name());
    }

    public CompletableFuture<Void> markNotified(String punishmentId) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.repository.updateDiscordNotified(punishmentId, true);
    }

    public CompletableFuture<Void> revoke(String punishmentId) {
        if (this.repository == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.repository.revoke(punishmentId);
    }

    public StatisticsManager getStatistics() {
        return this.statistics;
    }

    public void shutdown() {
        this.statistics.shutdown();
    }
}

