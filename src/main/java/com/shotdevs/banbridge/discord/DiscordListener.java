/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.dv8tion.jda.api.entities.User
 *  net.dv8tion.jda.api.events.guild.GuildBanEvent
 *  net.dv8tion.jda.api.events.guild.GuildUnbanEvent
 *  net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
 *  net.dv8tion.jda.api.hooks.ListenerAdapter
 */
package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.discord.ComponentV2Builder;
import com.shotdevs.banbridge.discord.DiscordNotifier;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.time.Instant;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordListener
extends ListenerAdapter {
    private final BanBridge plugin;

    public DiscordListener(BanBridge plugin) {
        this.plugin = plugin;
    }

    public void onGuildBan(GuildBanEvent event) {
        if (!this.plugin.getConfigManager().isNotifyDiscordBans()) {
            return;
        }
        User user = event.getUser();
        Instant now = Instant.now();
        Punishment punishment = new Punishment("db-" + user.getId() + "-" + now.getEpochSecond(), PunishmentType.DISCORD_BAN, user.getName(), null, user.getId(), "Unknown", null, null, "Discord ban issued", "PERMANENT", null, now, null, this.plugin.getConfigManager().getServerName(), PunishmentSource.DISCORD, true, null, false);
        this.plugin.getPunishmentManager().record(punishment);
        DiscordNotifier notifier = new DiscordNotifier(this.plugin, this.plugin.getDiscordManager(), new PunishmentRepository(this.plugin));
        notifier.notifyDiscord("bans", ComponentV2Builder.discordBan(user.getName(), user.getId(), "Unknown", "Discord ban", event.getGuild().getName(), now));
    }

    public void onGuildUnban(GuildUnbanEvent event) {
        if (!this.plugin.getConfigManager().isNotifyDiscordUnbans()) {
            return;
        }
        User user = event.getUser();
        Instant now = Instant.now();
        Punishment punishment = new Punishment("dub-" + user.getId() + "-" + now.getEpochSecond(), PunishmentType.DISCORD_UNBAN, user.getName(), null, user.getId(), "Unknown", null, null, "Discord unban", "UNKNOWN", null, now, null, this.plugin.getConfigManager().getServerName(), PunishmentSource.DISCORD, true, null, false);
        this.plugin.getPunishmentManager().record(punishment);
        DiscordNotifier notifier = new DiscordNotifier(this.plugin, this.plugin.getDiscordManager(), new PunishmentRepository(this.plugin));
        notifier.notifyDiscord("unbans", ComponentV2Builder.discordUnban(user.getName(), user.getId(), "Unknown", event.getGuild().getName(), now));
    }

    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
    }
}

