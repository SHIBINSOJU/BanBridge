/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerKickEvent
 *  org.bukkit.event.player.PlayerLoginEvent
 *  org.bukkit.event.player.PlayerLoginEvent$Result
 *  org.bukkit.event.player.PlayerPreLoginEvent$Result
 */
package com.shotdevs.banbridge.listeners;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.time.Instant;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;

public class PunishmentListener
implements Listener {
    private final BanBridge plugin;
    private final ConfigManager config;

    public PunishmentListener(BanBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        if (!this.config.isNotifyKicks()) {
            return;
        }
        Player player = event.getPlayer();
        String reason = event.getReason() == null ? "No reason provided" : event.getReason();
        Punishment punishment = new Punishment("native-kick-" + String.valueOf(player.getUniqueId()) + "-" + System.currentTimeMillis(), PunishmentType.KICK, player.getName(), player.getUniqueId(), null, "Unknown", null, null, reason, "UNKNOWN", null, Instant.now(), null, this.config.getServerName(), PunishmentSource.NATIVE, true, null, false);
        this.plugin.getPunishmentManager().record(punishment);
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.KICK_BANNED) {
            return;
        }
        if (!this.config.isNotifyBans()) {
            return;
        }
        Player player = event.getPlayer();
        String kickMessage = event.getKickMessage() == null ? "Banned" : event.getKickMessage();
        String reason = this.parseReason(kickMessage);
        boolean ipBan = kickMessage.toLowerCase().contains("ip");
        Punishment punishment = new Punishment("native-ban-join-" + String.valueOf(player.getUniqueId()) + "-" + System.currentTimeMillis(), ipBan ? PunishmentType.IP_BAN : PunishmentType.BAN, player.getName(), player.getUniqueId(), null, "Unknown", null, null, reason, "PERMANENT", null, Instant.now(), null, this.config.getServerName(), PunishmentSource.NATIVE, true, null, false);
        this.plugin.getPunishmentManager().record(punishment);
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getResult() != PlayerPreLoginEvent.Result.KICK_BANNED) {
            return;
        }
        if (!this.config.isNotifyBans()) {
            return;
        }
        String reason = this.parseReason(event.getKickMessage());
        boolean ipBan = event.getKickMessage().toLowerCase().contains("ip");
        Punishment punishment = new Punishment("native-ban-pre-" + String.valueOf(event.getUniqueId()) + "-" + System.currentTimeMillis(), ipBan ? PunishmentType.IP_BAN : PunishmentType.BAN, event.getName(), event.getUniqueId(), null, "Unknown", null, null, reason, "PERMANENT", null, Instant.now(), null, this.config.getServerName(), PunishmentSource.NATIVE, true, null, false);
        this.plugin.getPunishmentManager().record(punishment);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!(this.config.isAsyncProcessing() || this.config.isNotifyBans() || this.config.isNotifyKicks())) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.isBlank()) {
            return;
        }
        String[] parts = message.trim().split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String rest = parts.length > 1 ? parts[1] : "";
        Player issuer = event.getPlayer();
        String moderator = issuer.getName();
        UUID moderatorUuid = issuer.getUniqueId();
        switch (cmd) {
            case "/ban": 
            case "/ban-ip": {
                if (!this.config.isNotifyBans() && !this.config.isNotifyIpBans()) {
                    return;
                }
                String[] args = rest.split("\\s+", 2);
                String target = args[0];
                String reason = args.length > 1 ? args[1] : "No reason provided";
                boolean ip = cmd.equals("/ban-ip");
                Punishment p = new Punishment("native-cmd-" + String.valueOf(issuer.getUniqueId()) + "-" + System.currentTimeMillis(), ip ? PunishmentType.IP_BAN : PunishmentType.BAN, target, null, null, moderator, moderatorUuid, null, reason, "PERMANENT", null, Instant.now(), null, this.config.getServerName(), PunishmentSource.NATIVE, true, null, false);
                this.plugin.getPunishmentManager().record(p);
                break;
            }
            case "/pardon": 
            case "/pardon-ip": {
                if (!this.config.isNotifyUnbans()) {
                    return;
                }
                String[] args = rest.split("\\s+", 2);
                String target = args[0];
                Punishment p = new Punishment("native-cmd-" + String.valueOf(issuer.getUniqueId()) + "-" + System.currentTimeMillis(), PunishmentType.UNBAN, target, null, null, moderator, moderatorUuid, null, "Unbanned via command", "UNKNOWN", null, Instant.now(), null, this.config.getServerName(), PunishmentSource.NATIVE, true, null, false);
                this.plugin.getPunishmentManager().record(p);
                break;
            }
            case "/kick": {
                if (!this.config.isNotifyKicks()) {
                    return;
                }
                String[] args = rest.split("\\s+", 2);
                String target = args[0];
                String reason = args.length > 1 ? args[1] : "No reason provided";
                Player targetPlayer = this.plugin.getServer().getPlayer(target);
                UUID targetUuid = targetPlayer != null ? targetPlayer.getUniqueId() : null;
                Punishment p = new Punishment("native-cmd-" + String.valueOf(issuer.getUniqueId()) + "-" + System.currentTimeMillis(), PunishmentType.KICK, target, targetUuid, null, moderator, moderatorUuid, null, reason, "UNKNOWN", null, Instant.now(), null, this.config.getServerName(), PunishmentSource.NATIVE, true, null, false);
                this.plugin.getPunishmentManager().record(p);
            }
        }
    }

    private String parseReason(String kickMessage) {
        if (kickMessage == null) {
            return "No reason provided";
        }
        String m = kickMessage;
        if ((m = m.replaceAll("(?i)You are banned.*?\\.", "").trim()).isBlank()) {
            return "No reason provided";
        }
        return m;
    }
}

