package com.shotdevs.banbridge.punishment;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.NotificationQueue;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.discord.DiscordNotifier;
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
    private final Map<String, Long> recentPunishments = new HashMap<>();
    private static final long DEDUP_WINDOW_MS = 600_000L;

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

    public boolean record(Punishment punishment) {
        if (punishment == null) return false;

        String id = punishment.getId();
        String dedupKey = punishment.getType() + "|" + punishment.getTargetUsername()
                + "|" + punishment.getModeratorUsername() + "|" + punishment.getReason();
        long now = System.currentTimeMillis();

        synchronized (this.recentPunishments) {
            Long last = this.recentPunishments.get(dedupKey);
            if (last != null && now - last < DEDUP_WINDOW_MS) {
                return false;
            }
            this.recentPunishments.put(dedupKey, now);
        }

        this.evictOld();

        if (this.repository != null) {
            this.repository.insert(punishment).exceptionally(ex -> {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to persist punishment " + id, ex);
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

        this.plugin.getLogger().info("Recorded punishment: " + punishment.getType()
                + " on " + punishment.getTargetUsername() + " by " + punishment.getModeratorUsername());
        return true;
    }

    private void evictOld() {
        long cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS;
        this.recentPunishments.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    private boolean shouldNotify(Punishment p) {
        switch (p.getType()) {
            case BAN: return this.config.isNotifyBans();
            case TEMP_BAN: return this.config.isNotifyBans();
            case IP_BAN: return this.config.isNotifyIpBans();
            case TEMP_IP_BAN: return this.config.isNotifyIpBans();
            case UNBAN: return this.config.isNotifyUnbans();
            case IP_UNBAN: return this.config.isNotifyUnbans();
            case PERM_MUTE:
            case TEMP_MUTE: return this.config.isNotifyMutes();
            case UNMUTE: return this.config.isNotifyUnmutes();
            case WARNING: return this.config.isNotifyWarnings();
            case KICK: return this.config.isNotifyKicks();
            case FREEZE: return this.config.isNotifyFreezes();
            case UNFREEZE: return this.config.isNotifyUnfreezes();
            case JAIL: return this.config.isNotifyJails();
            case UNJAIL: return this.config.isNotifyUnjails();
            case DISCORD_BAN:
            case DISCORD_TEMP_BAN: return this.config.isNotifyDiscordBans();
            case DISCORD_UNBAN: return this.config.isNotifyDiscordUnbans();
            case DISCORD_KICK: return this.config.isNotifyDiscordKicks();
            case DISCORD_TIMEOUT: return this.config.isNotifyDiscordTimeouts();
            case DISCORD_TIMEOUT_REMOVAL: return this.config.isNotifyDiscordTimeoutRemovals();
            default: return true;
        }
    }

    public CompletableFuture<Optional<Punishment>> getPunishment(String id) {
        if (this.repository == null) return CompletableFuture.completedFuture(Optional.empty());
        return this.repository.findById(id);
    }

    public CompletableFuture<List<Punishment>> getHistory(String targetUsername) {
        if (this.repository == null) return CompletableFuture.completedFuture(List.of());
        return this.repository.findByTarget(targetUsername, null);
    }

    public CompletableFuture<List<Punishment>> getActive(String targetUsername) {
        if (this.repository == null) return CompletableFuture.completedFuture(List.of());
        return this.repository.findActiveByTarget(targetUsername);
    }

    public CompletableFuture<List<Punishment>> getByModerator(String moderatorUuid) {
        if (this.repository == null) return CompletableFuture.completedFuture(List.of());
        return this.repository.findByModerator(moderatorUuid);
    }

    public CompletableFuture<Integer> countWarnings(String targetUsername) {
        if (this.repository == null) return CompletableFuture.completedFuture(0);
        return this.repository.countByTypeAndTarget(targetUsername, PunishmentType.WARNING.name());
    }

    public CompletableFuture<Void> markNotified(String punishmentId) {
        if (this.repository == null) return CompletableFuture.completedFuture(null);
        return this.repository.updateDiscordNotified(punishmentId, true);
    }

    public CompletableFuture<Void> revoke(String punishmentId) {
        if (this.repository == null) return CompletableFuture.completedFuture(null);
        return this.repository.revoke(punishmentId);
    }

    public StatisticsManager getStatistics() {
        return this.statistics;
    }

    public void shutdown() {
        this.statistics.shutdown();
    }
}
