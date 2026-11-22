package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DiscordManager {
    private final BanBridge plugin;
    private final ConfigManager config;
    private volatile JDA jda;
    private CompletableFuture<JDA> connectionFuture;

    public DiscordManager(BanBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void connect() {
        if (!this.config.isDiscordEnabled()) {
            this.plugin.getLogger().info("Discord integration disabled in config");
            return;
        }
        String token = this.config.getBotToken();
        if (token == null || token.isBlank() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            this.plugin.getLogger().warning(
                    "Discord bot token is not configured. Discord features disabled.");
            return;
        }
        this.connectionFuture = new CompletableFuture<>();
        try {
            this.jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_MODERATION,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .addEventListeners(
                            new DiscordListener(this.plugin),
                            new com.shotdevs.banbridge.discord.interactions.ButtonHandler(this.plugin))
                    .build();
            this.connectionFuture.complete(this.jda);
            this.plugin.getLogger().info(
                    "Discord bot connected as " + this.jda.getSelfUser().getName());
        } catch (Exception ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to connect to Discord", ex);
            this.connectionFuture.completeExceptionally(ex);
        }
    }

    public boolean isConnected() {
        return this.jda != null && this.jda.getStatus() == JDA.Status.CONNECTED;
    }

    public JDA getJDA() {
        return this.jda;
    }

    public CompletableFuture<JDA> getConnectionFuture() {
        if (this.connectionFuture == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.connectionFuture;
    }

    public TextChannel getTextChannel(String channelId) {
        if (this.jda == null || channelId == null || channelId.isBlank()) return null;
        String guildId = this.config.getGuildId();
        if (guildId != null && !guildId.isBlank()) {
            Guild g = this.jda.getGuildById(guildId);
            if (g != null) {
                return g.getTextChannelById(channelId);
            }
        }
        return null;
    }

    public Guild getGuild() {
        String gid = this.config.getGuildId();
        if (this.jda == null || gid == null || gid.isBlank()) return null;
        return this.jda.getGuildById(gid);
    }

    public void shutdown() {
        if (this.jda != null) {
            try {
                this.jda.shutdown();
            } catch (Exception ignored) {}
            this.jda = null;
        }
    }
}
