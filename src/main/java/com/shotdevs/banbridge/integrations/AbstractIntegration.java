/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.shotdevs.banbridge.integrations;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.integrations.Integration;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public abstract class AbstractIntegration
implements Integration {
    protected final BanBridge plugin;
    protected final ConfigManager config;
    protected final String pluginName;
    protected volatile boolean enabled;

    protected AbstractIntegration(BanBridge plugin, String pluginName, String configKey) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.pluginName = pluginName;
        this.enabled = this.readEnabled(configKey);
    }

    private boolean readEnabled(String key) {
        return switch (key) {
            case "litebans" -> this.config.isIntegrationLiteBans();
            case "advancedban" -> this.config.isIntegrationAdvancedBan();
            case "libertybans" -> this.config.isIntegrationLibertyBans();
            case "essentialsx" -> this.config.isIntegrationEssentialsX();
            case "cmi" -> this.config.isIntegrationCmi();
            default -> true;
        };
    }

    @Override
    public boolean isInstalled() {
        Plugin p = this.plugin.getServer().getPluginManager().getPlugin(this.pluginName);
        return p != null && p.isEnabled();
    }

    @Override
    public String getVersion() {
        Plugin p = this.plugin.getServer().getPluginManager().getPlugin(this.pluginName);
        return p != null ? p.getDescription().getVersion() : "N/A";
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.plugin.getLogger().info("Integration enabled: " + this.name() + " (" + this.getVersion() + ")");
    }

    @Override
    public void disable() {
        this.enabled = false;
        this.plugin.getLogger().info("Integration disabled: " + this.name());
    }

    protected Optional<Punishment> fromData(Map<String, Object> data, PunishmentSource source) {
        try {
            PunishmentType type;
            String id = String.valueOf(data.getOrDefault("id", UUID.randomUUID().toString()));
            String typeName = String.valueOf(data.getOrDefault("type", "WARNING"));
            try {
                type = PunishmentType.valueOf(typeName.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                type = PunishmentType.WARNING;
            }
            String target = String.valueOf(data.getOrDefault("target", "UNKNOWN"));
            String targetUuidS = String.valueOf(data.getOrDefault("targetUuid", ""));
            UUID targetUuid = targetUuidS.isBlank() ? null : UUID.fromString(targetUuidS);
            String moderator = String.valueOf(data.getOrDefault("moderator", "UNKNOWN"));
            String modUuidS = String.valueOf(data.getOrDefault("moderatorUuid", ""));
            UUID modUuid = modUuidS.isBlank() ? null : UUID.fromString(modUuidS);
            String reason = String.valueOf(data.getOrDefault("reason", "No reason provided"));
            String duration = String.valueOf(data.getOrDefault("duration", "UNKNOWN"));
            Long durationSec = null;
            Object ds = data.get("durationSeconds");
            if (ds instanceof Number) {
                Number n = (Number)ds;
                durationSec = n.longValue();
            }
            String server = String.valueOf(data.getOrDefault("server", this.plugin.getConfigManager().getServerName()));
            return Optional.of(new Punishment(id, type, target, targetUuid, String.valueOf(data.getOrDefault("targetDiscordId", "")), moderator, modUuid, String.valueOf(data.getOrDefault("moderatorDiscordId", "")), reason, duration, durationSec, Instant.now(), null, server, source, true, String.valueOf(data.getOrDefault("evidenceUrl", "")), false));
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to map integration data to Punishment", ex);
            return Optional.empty();
        }
    }
}

