/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.integrations;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.integrations.Integration;
import com.shotdevs.banbridge.integrations.advancedban.AdvancedBanIntegration;
import com.shotdevs.banbridge.integrations.cmi.CMIIntegration;
import com.shotdevs.banbridge.integrations.essentialsx.EssentialsXIntegration;
import com.shotdevs.banbridge.integrations.libertybans.LibertyBansIntegration;
import com.shotdevs.banbridge.integrations.litebans.LiteBansIntegration;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class IntegrationManager {
    private final BanBridge plugin;
    private final ConfigManager config;
    private final Map<String, Integration> integrations = new ConcurrentHashMap<String, Integration>();
    private final Set<String> processedEventKeys = ConcurrentHashMap.newKeySet();

    public IntegrationManager(BanBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void detectIntegrations() {
        this.register(new LiteBansIntegration(this.plugin));
        this.register(new AdvancedBanIntegration(this.plugin));
        this.register(new LibertyBansIntegration(this.plugin));
        this.register(new EssentialsXIntegration(this.plugin));
        this.register(new CMIIntegration(this.plugin));
        this.plugin.getLogger().info("Integration detection complete: " + this.integrations.size() + " adapters loaded");
        this.integrations.values().forEach(i -> this.plugin.getLogger().info("  " + i.name() + ": " + (String)(i.isInstalled() ? "DETECTED v" + i.getVersion() : "not installed") + " | enabled=" + i.isEnabled() + " | supports=" + String.valueOf(i.getSupportedTypes())));
    }

    private void register(Integration integration) {
        this.integrations.put(integration.name(), integration);
        if (integration.isInstalled() && integration.isEnabled()) {
            try {
                integration.enable();
                this.plugin.getLogger().info("Enabled integration: " + integration.name());
            }
            catch (Exception ex) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to enable integration " + integration.name(), ex);
            }
        }
    }

    public void reload() {
        this.integrations.values().forEach(Integration::disable);
        this.integrations.clear();
        this.processedEventKeys.clear();
        this.detectIntegrations();
    }

    public Optional<Integration> get(String name) {
        return Optional.ofNullable(this.integrations.get(name));
    }

    public Collection<Integration> getAll() {
        return this.integrations.values();
    }

    public Map<String, IntegrationStatus> getStatuses() {
        LinkedHashMap<String, IntegrationStatus> statuses = new LinkedHashMap<String, IntegrationStatus>();
        for (Integration i : this.integrations.values()) {
            statuses.put(i.name(), new IntegrationStatus(i.name(), i.isInstalled(), i.isInstalled() ? i.getVersion() : null, i.isEnabled(), i.getSupportedTypes()));
        }
        return statuses;
    }

    public boolean isIntegrationEnabled(String name) {
        Integration i = this.integrations.get(name);
        return i != null && i.isEnabled() && i.isInstalled();
    }

    public Optional<Punishment> handleEvent(String integrationName, Object rawEvent, String eventKey) {
        if (eventKey != null && !this.processedEventKeys.add(eventKey)) {
            return Optional.empty();
        }
        Integration integration = this.integrations.get(integrationName);
        if (integration == null || !integration.isEnabled() || !integration.isInstalled()) {
            return Optional.empty();
        }
        return integration.toPunishment(rawEvent);
    }

    public void clearDedup(String key) {
        this.processedEventKeys.remove(key);
    }

    public record IntegrationStatus(String name, boolean installed, String version, boolean enabled, List<PunishmentType> supportedTypes) {
    }
}

