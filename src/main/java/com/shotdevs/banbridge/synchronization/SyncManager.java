/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.synchronization;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncManager {
    private final BanBridge plugin;
    private final ConfigManager config;
    private final Map<String, Long> executedActions = new ConcurrentHashMap<String, Long>();
    private static final long EXECUTION_WINDOW_MS = 600000L;

    public SyncManager(BanBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public boolean isMinecraftToDiscordEnabled() {
        return this.config.isMinecraftToDiscord();
    }

    public boolean isDiscordToMinecraftEnabled() {
        return this.config.isDiscordToMinecraft();
    }

    public boolean isLoopPreventionEnabled() {
        return true;
    }

    public void markExecuted(String actionId) {
        this.executedActions.put(actionId, System.currentTimeMillis());
        this.evictOld();
    }

    public boolean wasExecuted(String actionId) {
        this.evictOld();
        return this.executedActions.containsKey(actionId);
    }

    private void evictOld() {
        long cutoff = System.currentTimeMillis() - 600000L;
        this.executedActions.entrySet().removeIf(e -> (Long)e.getValue() < cutoff);
    }
}

