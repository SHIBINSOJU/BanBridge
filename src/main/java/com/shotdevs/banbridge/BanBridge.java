/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.shotdevs.banbridge;

import com.shotdevs.banbridge.commands.BanBridgeCommand;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.DatabaseManager;
import com.shotdevs.banbridge.database.NotificationQueue;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.discord.DiscordCommandManager;
import com.shotdevs.banbridge.discord.DiscordManager;
import com.shotdevs.banbridge.discord.DiscordNotifier;
import com.shotdevs.banbridge.integrations.IntegrationManager;
import com.shotdevs.banbridge.listeners.PunishmentListener;
import com.shotdevs.banbridge.punishment.PunishmentManager;
import com.shotdevs.banbridge.statistics.StatisticsManager;
import com.shotdevs.banbridge.synchronization.SyncManager;
import java.util.logging.Level;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class BanBridge
extends JavaPlugin {
    public static BanBridge instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PunishmentManager punishmentManager;
    private IntegrationManager integrationManager;
    private DiscordManager discordManager;
    private DiscordNotifier discordNotifier;
    private DiscordCommandManager discordCommandManager;
    private PunishmentListener punishmentListener;
    private SyncManager syncManager;
    private NotificationQueue notificationQueue;
    private StatisticsManager statistics;

    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.configManager.load();
        this.databaseManager = new DatabaseManager(this);
        this.statistics = new StatisticsManager(this);
        this.integrationManager = new IntegrationManager(this);
        this.syncManager = new SyncManager(this);
        this.discordManager = new DiscordManager(this);
        PunishmentRepository repository = new PunishmentRepository(this);
        this.discordNotifier = new DiscordNotifier(this, this.discordManager, repository);
        this.notificationQueue = new NotificationQueue(this, this.discordNotifier);
        this.punishmentManager = new PunishmentManager(this);
        this.punishmentManager.wire(this.discordNotifier, this.notificationQueue);
        this.punishmentListener = new PunishmentListener(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.punishmentListener, (Plugin)this);
        BanBridgeCommand banbridgeCommand = new BanBridgeCommand(this);
        this.getCommand("banbridge").setExecutor((CommandExecutor)banbridgeCommand);
        this.getCommand("banbridge").setTabCompleter((TabCompleter)banbridgeCommand);
        this.getServer().getScheduler().runTaskAsynchronously((Plugin)this, () -> {
            try {
                this.databaseManager.initialize();
                if (this.configManager.isDiscordEnabled()) {
                    this.discordManager.connect();
                }
                this.integrationManager.detectIntegrations();
                this.discordManager.getConnectionFuture().thenAccept(jda -> {
                    if (jda != null) {
                        this.discordCommandManager = new DiscordCommandManager(this, this.discordManager, this.discordNotifier, repository, this.statistics);
                        jda.addEventListener(new Object[]{this.discordCommandManager});
                        this.discordCommandManager.registerCommands();
                        this.getLogger().info("Discord slash commands registered");
                    }
                });
                this.getLogger().info("BanBridge async initialization complete");
            }
            catch (Exception ex) {
                this.getLogger().log(Level.SEVERE, "BanBridge initialization error", ex);
            }
        });
        this.getLogger().info("BanBridge 2.0 enabled by ShotDevs");
    }

    public void onDisable() {
        this.getLogger().info("Shutting down BanBridge 2.0...");
        if (this.discordCommandManager != null) {
            this.discordCommandManager.shutdown();
        }
        if (this.discordManager != null) {
            this.discordManager.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        if (this.punishmentManager != null) {
            this.punishmentManager.shutdown();
        }
        if (this.statistics != null) {
            this.statistics.shutdown();
        }
        this.getLogger().info("BanBridge 2.0 disabled");
    }

    public static BanBridge getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public PunishmentManager getPunishmentManager() {
        return this.punishmentManager;
    }

    public IntegrationManager getIntegrationManager() {
        return this.integrationManager;
    }

    public DiscordManager getDiscordManager() {
        return this.discordManager;
    }

    public DiscordNotifier getDiscordNotifier() {
        return this.discordNotifier;
    }

    public DiscordCommandManager getDiscordCommandManager() {
        return this.discordCommandManager;
    }

    public NotificationQueue getNotificationQueue() {
        return this.notificationQueue;
    }

    public SyncManager getSyncManager() {
        return this.syncManager;
    }

    public StatisticsManager getStatistics() {
        return this.statistics;
    }
}

