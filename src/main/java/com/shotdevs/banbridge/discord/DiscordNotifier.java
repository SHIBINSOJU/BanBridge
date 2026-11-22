/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.dv8tion.jda.api.components.MessageTopLevelComponent
 *  net.dv8tion.jda.api.components.container.Container
 *  net.dv8tion.jda.api.entities.channel.concrete.TextChannel
 *  net.dv8tion.jda.api.requests.restaction.MessageCreateAction
 */
package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.discord.ComponentV2Builder;
import com.shotdevs.banbridge.discord.DiscordManager;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.util.Optional;
import java.util.logging.Level;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

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
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to send queued notification " + punishmentId, ex);
            return false;
        }
    }

    private Container build(Punishment p) {
        return switch (p.getType()) {
            case PunishmentType.BAN, PunishmentType.TEMP_BAN, PunishmentType.IP_BAN, PunishmentType.TEMP_IP_BAN -> {
                Container var2_2;
                yield var2_2 = ComponentV2Builder.ban(p);
            }
            case PunishmentType.PERM_MUTE, PunishmentType.TEMP_MUTE -> {
                Container var2_3;
                yield var2_3 = ComponentV2Builder.mute(p);
            }
            case PunishmentType.WARNING -> {
                Container var2_4;
                int total = 0;
                try {
                    total = this.repository.countWarnings(p.getTargetUsername()).join();
                }
                catch (Exception var4_14) {
                    // empty catch block
                }
                yield var2_4 = ComponentV2Builder.warning(p, total);
            }
            case PunishmentType.KICK -> {
                Container var2_5;
                yield var2_5 = ComponentV2Builder.kick(p);
            }
            case PunishmentType.FREEZE -> {
                Container var2_6;
                yield var2_6 = ComponentV2Builder.freeze(p);
            }
            case PunishmentType.UNFREEZE -> {
                Container var2_7;
                yield var2_7 = ComponentV2Builder.unfreeze(p);
            }
            case PunishmentType.JAIL -> {
                Container var2_8;
                yield var2_8 = ComponentV2Builder.jail(p);
            }
            case PunishmentType.UNJAIL -> {
                Container var2_9;
                yield var2_9 = ComponentV2Builder.unjail(p);
            }
            case PunishmentType.UNBAN, PunishmentType.IP_UNBAN -> {
                Container var2_10;
                yield var2_10 = ComponentV2Builder.unban(p, p.getReason(), p.getDuration());
            }
            case PunishmentType.UNMUTE -> {
                Container var2_11;
                yield var2_11 = ComponentV2Builder.unmute(p, p.getReason(), p.getDuration());
            }
            default -> {
                Container var2_12;
                yield var2_12 = null;
            }
        };
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
        MessageCreateAction action = targetChannel.sendMessageComponents((MessageTopLevelComponent)data, new MessageTopLevelComponent[0]);
        action.queue(sent -> this.plugin.getLogger().fine("Discord notification sent to " + targetChannel.getName()), ex -> this.plugin.getLogger().log(Level.WARNING, "Failed to send Discord notification", (Throwable)ex));
    }

    private String resolveChannel(PunishmentType type) {
        return switch (type.getCategory()) {
            default -> throw new MatchException(null, null);
            case PunishmentType.Category.BAN -> this.config.getChannelBans();
            case PunishmentType.Category.CHAT -> this.config.getChannelMutes();
            case PunishmentType.Category.PLAYER -> this.config.getChannelKicks();
            case PunishmentType.Category.DISCORD -> this.config.getChannelOther();
        };
    }

    private String resolveDiscordChannel(String category) {
        return switch (category) {
            case "bans" -> this.config.getChannelBans();
            case "kicks" -> this.config.getChannelKicks();
            default -> this.config.getChannelOther();
        };
    }

    public TextChannel getChannel(String channelId) {
        return this.discord.getTextChannel(channelId);
    }
}

