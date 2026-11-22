/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.shotdevs.banbridge.configuration;

import com.shotdevs.banbridge.BanBridge;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final BanBridge plugin;
    private FileConfiguration config;
    private File configFile;
    private boolean discordEnabled;
    private String botToken;
    private String guildId;
    private final Set<String> staffRoleIds = new HashSet<String>();
    private String channelBans;
    private String channelMutes;
    private String channelWarnings;
    private String channelKicks;
    private String channelOther;
    private String serverName;
    private ZoneId timezone;
    private boolean minecraftToDiscord;
    private boolean discordToMinecraft;
    private boolean notifyBans;
    private boolean notifyUnbans;
    private boolean notifyIpBans;
    private boolean notifyMutes;
    private boolean notifyUnmutes;
    private boolean notifyWarnings;
    private boolean notifyKicks;
    private boolean notifyFreezes;
    private boolean notifyUnfreezes;
    private boolean notifyJails;
    private boolean notifyUnjails;
    private boolean notifyDiscordBans;
    private boolean notifyDiscordUnbans;
    private boolean notifyDiscordKicks;
    private boolean notifyDiscordTimeouts;
    private boolean notifyDiscordTimeoutRemovals;
    private boolean integrationLiteBans;
    private boolean integrationAdvancedBan;
    private boolean integrationLibertyBans;
    private boolean integrationEssentialsX;
    private boolean integrationCmi;
    private String storageType;
    private boolean mysqlEnabled;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private String footer;
    private boolean hideIpAddresses;
    private boolean showPlayerUuid;
    private boolean showModeratorUuid;
    private boolean asyncProcessing;
    private boolean retryFailedMessages;
    private int maxRetries;
    private int queueLimit;

    public ConfigManager(BanBridge plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.load();
    }

    public void load() {
        if (!this.configFile.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.configFile);
        try (InputStream def = this.plugin.getResource("config.yml");){
            if (def != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(def, StandardCharsets.UTF_8));
                this.config.setDefaults((Configuration)defaults);
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to load embedded config defaults", ex);
        }
        this.loadValues();
    }

    public void reload() {
        this.load();
    }

    private void loadValues() {
        this.discordEnabled = this.config.getBoolean("discord.enabled", false);
        this.botToken = this.config.getString("discord.bot-token", "");
        this.guildId = this.config.getString("discord.guild-id", "");
        this.staffRoleIds.clear();
        this.staffRoleIds.addAll(this.config.getStringList("discord.staff-roles"));
        this.channelBans = this.config.getString("channels.bans", "");
        this.channelMutes = this.config.getString("channels.mutes", "");
        this.channelWarnings = this.config.getString("channels.warnings", "");
        this.channelKicks = this.config.getString("channels.kicks", "");
        this.channelOther = this.config.getString("channels.other", "");
        this.serverName = this.config.getString("server.name", "Survival");
        String tz = this.config.getString("server.timezone", "UTC");
        try {
            this.timezone = ZoneId.of(tz);
        }
        catch (Exception ex) {
            this.timezone = ZoneId.of("UTC");
            this.plugin.getLogger().warning("Invalid timezone '" + tz + "', falling back to UTC");
        }
        this.minecraftToDiscord = this.config.getBoolean("notifications.minecraft-to-discord", true);
        this.discordToMinecraft = this.config.getBoolean("notifications.discord-to-minecraft", false);
        this.notifyBans = this.config.getBoolean("notifications.bans", true);
        this.notifyUnbans = this.config.getBoolean("notifications.unbans", true);
        this.notifyIpBans = this.config.getBoolean("notifications.ip-bans", true);
        this.notifyMutes = this.config.getBoolean("notifications.mutes", true);
        this.notifyUnmutes = this.config.getBoolean("notifications.unmutes", true);
        this.notifyWarnings = this.config.getBoolean("notifications.warnings", true);
        this.notifyKicks = this.config.getBoolean("notifications.kicks", true);
        this.notifyFreezes = this.config.getBoolean("notifications.freezes", true);
        this.notifyUnfreezes = this.config.getBoolean("notifications.unfreezes", true);
        this.notifyJails = this.config.getBoolean("notifications.jails", true);
        this.notifyUnjails = this.config.getBoolean("notifications.unjails", true);
        this.notifyDiscordBans = this.config.getBoolean("notifications.discord-bans", true);
        this.notifyDiscordUnbans = this.config.getBoolean("notifications.discord-unbans", true);
        this.notifyDiscordKicks = this.config.getBoolean("notifications.discord-kicks", true);
        this.notifyDiscordTimeouts = this.config.getBoolean("notifications.discord-timeouts", true);
        this.notifyDiscordTimeoutRemovals = this.config.getBoolean("notifications.discord-timeout-removals", true);
        this.integrationLiteBans = this.config.getBoolean("integrations.litebans", true);
        this.integrationAdvancedBan = this.config.getBoolean("integrations.advancedban", true);
        this.integrationLibertyBans = this.config.getBoolean("integrations.libertybans", true);
        this.integrationEssentialsX = this.config.getBoolean("integrations.essentialsx", true);
        this.integrationCmi = this.config.getBoolean("integrations.cmi", true);
        this.storageType = this.config.getString("storage.type", "sqlite");
        this.mysqlEnabled = this.config.getBoolean("storage.mysql.enabled", false);
        this.mysqlHost = this.config.getString("storage.mysql.host", "localhost");
        this.mysqlPort = this.config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = this.config.getString("storage.mysql.database", "banbridge");
        this.mysqlUsername = this.config.getString("storage.mysql.username", "root");
        this.mysqlPassword = this.config.getString("storage.mysql.password", "");
        this.footer = this.config.getString("appearance.footer", "BanBridge | ShotDevs Moderation System");
        this.hideIpAddresses = this.config.getBoolean("privacy.hide-ip-addresses", true);
        this.showPlayerUuid = this.config.getBoolean("privacy.show-player-uuid", true);
        this.showModeratorUuid = this.config.getBoolean("privacy.show-moderator-uuid", false);
        this.asyncProcessing = this.config.getBoolean("performance.async-processing", true);
        this.retryFailedMessages = this.config.getBoolean("performance.retry-failed-messages", true);
        this.maxRetries = this.config.getInt("performance.max-retries", 3);
        this.queueLimit = this.config.getInt("performance.queue-limit", 10000);
    }

    public void save() {
        try {
            this.config.save(this.configFile);
        }
        catch (IOException ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to save config.yml", ex);
        }
    }

    public boolean isDiscordEnabled() {
        return this.discordEnabled;
    }

    public String getBotToken() {
        return this.botToken;
    }

    public String getGuildId() {
        return this.guildId;
    }

    public Set<String> getStaffRoleIds() {
        return this.staffRoleIds;
    }

    public String getChannelBans() {
        return this.channelBans;
    }

    public String getChannelMutes() {
        return this.channelMutes;
    }

    public String getChannelWarnings() {
        return this.channelWarnings;
    }

    public String getChannelKicks() {
        return this.channelKicks;
    }

    public String getChannelOther() {
        return this.channelOther;
    }

    public String getChannelForCategory(String category) {
        return switch (category) {
            case "bans" -> this.channelBans;
            case "mutes" -> this.channelMutes;
            case "warnings" -> this.channelWarnings;
            case "kicks" -> this.channelKicks;
            default -> this.channelOther;
        };
    }

    public String getServerName() {
        return this.serverName;
    }

    public ZoneId getTimezone() {
        return this.timezone;
    }

    public boolean isMinecraftToDiscord() {
        return this.minecraftToDiscord;
    }

    public boolean isDiscordToMinecraft() {
        return this.discordToMinecraft;
    }

    public boolean isNotifyBans() {
        return this.notifyBans;
    }

    public boolean isNotifyUnbans() {
        return this.notifyUnbans;
    }

    public boolean isNotifyIpBans() {
        return this.notifyIpBans;
    }

    public boolean isNotifyMutes() {
        return this.notifyMutes;
    }

    public boolean isNotifyUnmutes() {
        return this.notifyUnmutes;
    }

    public boolean isNotifyWarnings() {
        return this.notifyWarnings;
    }

    public boolean isNotifyKicks() {
        return this.notifyKicks;
    }

    public boolean isNotifyFreezes() {
        return this.notifyFreezes;
    }

    public boolean isNotifyUnfreezes() {
        return this.notifyUnfreezes;
    }

    public boolean isNotifyJails() {
        return this.notifyJails;
    }

    public boolean isNotifyUnjails() {
        return this.notifyUnjails;
    }

    public boolean isNotifyDiscordBans() {
        return this.notifyDiscordBans;
    }

    public boolean isNotifyDiscordUnbans() {
        return this.notifyDiscordUnbans;
    }

    public boolean isNotifyDiscordKicks() {
        return this.notifyDiscordKicks;
    }

    public boolean isNotifyDiscordTimeouts() {
        return this.notifyDiscordTimeouts;
    }

    public boolean isNotifyDiscordTimeoutRemovals() {
        return this.notifyDiscordTimeoutRemovals;
    }

    public boolean isIntegrationLiteBans() {
        return this.integrationLiteBans;
    }

    public boolean isIntegrationAdvancedBan() {
        return this.integrationAdvancedBan;
    }

    public boolean isIntegrationLibertyBans() {
        return this.integrationLibertyBans;
    }

    public boolean isIntegrationEssentialsX() {
        return this.integrationEssentialsX;
    }

    public boolean isIntegrationCmi() {
        return this.integrationCmi;
    }

    public String getStorageType() {
        return this.storageType;
    }

    public boolean isMysqlEnabled() {
        return this.mysqlEnabled;
    }

    public String getMysqlHost() {
        return this.mysqlHost;
    }

    public int getMysqlPort() {
        return this.mysqlPort;
    }

    public String getMysqlDatabase() {
        return this.mysqlDatabase;
    }

    public String getMysqlUsername() {
        return this.mysqlUsername;
    }

    public String getMysqlPassword() {
        return this.mysqlPassword;
    }

    public String getFooter() {
        return this.footer;
    }

    public boolean isHideIpAddresses() {
        return this.hideIpAddresses;
    }

    public boolean isShowPlayerUuid() {
        return this.showPlayerUuid;
    }

    public boolean isShowModeratorUuid() {
        return this.showModeratorUuid;
    }

    public boolean isAsyncProcessing() {
        return this.asyncProcessing;
    }

    public boolean isRetryFailedMessages() {
        return this.retryFailedMessages;
    }

    public int getMaxRetries() {
        return this.maxRetries;
    }

    public int getQueueLimit() {
        return this.queueLimit;
    }

    public FileConfiguration getRawConfig() {
        return this.config;
    }
}

