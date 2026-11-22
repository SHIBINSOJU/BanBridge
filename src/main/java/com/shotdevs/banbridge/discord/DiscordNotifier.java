package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentType;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.util.Optional;
import java.util.logging.Level;

public class DiscordNotifier {
    private final BanBridge plugin;
    private final DiscordManager discord;
    private final ConfigManager config;
    private final PunishmentRepository repository;

    public DiscordNotifier(BanBridge plugin, DiscordManager discord, PunishmentRepository repository) {
        this.plugin = plugin;
        this.discord = discord;
        this.config = plugin.getConfigManager();
        this.repository = repository;
    }

    public void notify(Punishment punishment) {
        if (!this.config.isMinecraftToDiscord() || this.discord.getJDA() == null) {
            return;
        }
        Container data = this.build(punishment);
        if (data == null) {
            return;
        }
        this.sendAsync(this.resolveChannel(punishment.getType()), data);
        this.repository.updateDiscordNotified(punishment.getId(), true);
    }

    public void notifyDiscord(String category, Container data) {
        if (!this.config.isMinecraftToDiscord() || this.discord.getJDA() == null) {
            return;
        }
        this.sendAsync(this.resolveDiscordChannel(category), data);
    }

    public boolean sendQueued(String punishmentId) {
        if (this.discord.getJDA() == null) {
            return false;
        }
        try {
            Optional<Punishment> opt = this.repository.findById(punishmentId).join();
            if (opt.isEmpty()) {
                return true;
            }
            Punishment p = opt.get();
            if (p.isDiscordNotified()) {
                return true;
            }
            this.notify(p);
            return true;
        } catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to send queued notification " + punishmentId, ex);
            return false;
        }
    }

    private Container build(Punishment p) {
        switch (p.getType()) {
            case BAN:
            case TEMP_BAN:
            case IP_BAN:
            case TEMP_IP_BAN:
                return ComponentV2Builder.ban(p);
            case PERM_MUTE:
            case TEMP_MUTE:
                return ComponentV2Builder.mute(p);
            case WARNING: {
                int total = 0;
                try {
                    total = this.repository.countWarnings(p.getTargetUsername()).join();
                } catch (Exception ignored) {}
                return ComponentV2Builder.warning(p, total);
            }
            case KICK:
                return ComponentV2Builder.kick(p);
            case FREEZE:
                return ComponentV2Builder.freeze(p);
            case UNFREEZE:
                return ComponentV2Builder.unfreeze(p);
            case JAIL:
                return ComponentV2Builder.jail(p);
            case UNJAIL:
                return ComponentV2Builder.unjail(p);
            case UNBAN:
            case IP_UNBAN:
                return ComponentV2Builder.unban(p, p.getReason(), p.getDuration());
            case UNMUTE:
                return ComponentV2Builder.unmute(p, p.getReason(), p.getDuration());
            default:
                return null;
        }
    }

    private void sendAsync(String channelId, Container data) {
        TextChannel channel = this.discord.getTextChannel(channelId);
        if (channel == null) {
            channel = this.discord.getTextChannel(this.config.getChannelOther());
        }
        if (channel == null) {
            this.plugin.getLogger().warning("No Discord channel configured for notifications");
            return;
        }
        TextChannel targetChannel = channel;
        MessageCreateAction action = targetChannel.sendMessageComponents(
                (MessageTopLevelComponent) data, new MessageTopLevelComponent[0]);
        action.queue(
                sent -> this.plugin.getLogger().fine("Discord notification sent to " + targetChannel.getName()),
                ex -> this.plugin.getLogger().log(Level.WARNING, "Failed to send Discord notification", ex)
        );
    }

    private String resolveChannel(PunishmentType type) {
        switch (type.getCategory()) {
            case BAN: return this.config.getChannelBans();
            case CHAT: return this.config.getChannelMutes();
            case PLAYER: return this.config.getChannelKicks();
            case DISCORD: return this.config.getChannelOther();
            default: return this.config.getChannelOther();
        }
    }

    private String resolveDiscordChannel(String category) {
        switch (category) {
            case "bans": return this.config.getChannelBans();
            case "kicks": return this.config.getChannelKicks();
            default: return this.config.getChannelOther();
        }
    }

    public TextChannel getChannel(String channelId) {
        return this.discord.getTextChannel(channelId);
    }
}
