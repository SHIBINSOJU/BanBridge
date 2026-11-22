package com.shotdevs.banbridge.commands;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.NotificationQueue;
import com.shotdevs.banbridge.discord.ComponentV2Builder;
import com.shotdevs.banbridge.integrations.IntegrationManager;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;

import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BanBridgeCommand implements CommandExecutor, TabCompleter {
    private final BanBridge plugin;

    public BanBridgeCommand(BanBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("banbridge.admin")) {
            sender.sendMessage("\u00a7cYou do not have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            this.showHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help":       this.showHelp(sender); break;
            case "version":    this.showVersion(sender); break;
            case "reload":     this.handleReload(sender); break;
            case "status":     this.handleStatus(sender); break;
            case "integrations": this.handleIntegrations(sender); break;
            case "test":       this.handleTest(sender); break;
            case "history":    this.handleHistory(sender, args); break;
            case "lookup":     this.handleLookup(sender, args); break;
            case "stats":      this.handleStats(sender, args); break;
            case "queue":      this.handleQueue(sender); break;
            default:           this.showHelp(sender); break;
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("\u00a77=== \u00a7fBanBridge 2.0 \u00a77(by ShotDevs) ===");
        sender.sendMessage("\u00a77/banbridge help        \u00a7f- Show this help");
        sender.sendMessage("\u00a77/banbridge version     \u00a7f- Show plugin version");
        sender.sendMessage("\u00a77/banbridge reload      \u00a7f- Reload configuration");
        sender.sendMessage("\u00a77/banbridge status      \u00a7f- Show plugin status");
        sender.sendMessage("\u00a77/banbridge integrations\u00a7f- Show integration status");
        sender.sendMessage("\u00a77/banbridge test        \u00a7f- Send a test notification");
        sender.sendMessage("\u00a77/banbridge history <player> \u00a7f- View punishment history");
        sender.sendMessage("\u00a77/banbridge lookup <player>  \u00a7f- Lookup active punishments");
        sender.sendMessage("\u00a77/banbridge stats [mod] \u00a7f- View staff statistics");
        sender.sendMessage("\u00a77/banbridge queue       \u00a7f- Show notification queue");
    }

    private void showVersion(CommandSender sender) {
        sender.sendMessage("\u00a7fBanBridge 2.0 \u00a77- version " + this.plugin.getDescription().getVersion());
        sender.sendMessage("\u00a77by ShotDevs - Minecraft <-> Discord Moderation Sync");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("banbridge.reload")) {
            sender.sendMessage("\u00a7cMissing permission: banbridge.reload");
            return;
        }
        this.plugin.getConfigManager().reload();
        this.plugin.getIntegrationManager().reload();
        if (this.plugin.getDiscordCommandManager() != null) {
            this.plugin.getDiscordCommandManager().reload();
        }
        sender.sendMessage("\u00a7aBanBridge configuration reloaded.");
    }

    private void handleStatus(CommandSender sender) {
        ConfigManager cfg = this.plugin.getConfigManager();
        sender.sendMessage("\u00a77=== BanBridge Status ===");
        sender.sendMessage("\u00a7fDiscord: \u00a77" + (cfg.isDiscordEnabled() ? "enabled" : "disabled"));
        sender.sendMessage("\u00a7fConnected: \u00a77" + (this.plugin.getDiscordManager() != null
                && this.plugin.getDiscordManager().isConnected()));
        sender.sendMessage("\u00a7fMinecraft->Discord: \u00a77" + cfg.isMinecraftToDiscord());
        sender.sendMessage("\u00a7fDiscord->Minecraft: \u00a77" + cfg.isDiscordToMinecraft());
        sender.sendMessage("\u00a7fStorage: \u00a77" + cfg.getStorageType());
        sender.sendMessage("\u00a7fServer: \u00a77" + cfg.getServerName());
    }

    private void handleIntegrations(CommandSender sender) {
        sender.sendMessage("\u00a77=== BanBridge Integrations ===");
        for (IntegrationManager.IntegrationStatus s : this.plugin.getIntegrationManager().getStatuses().values()) {
            String state = s.installed()
                    ? (s.enabled() ? "\u00a7aEnabled" : "\u00a7cDisabled")
                    : "\u00a77Not installed";
            String ver = s.version() != null ? " \u00a77(v" + s.version() + ")" : "";
            sender.sendMessage("\u00a7f" + s.name() + ": \u00a77" + state + ver);
        }
    }

    private void handleTest(CommandSender sender) {
        if (!sender.hasPermission("banbridge.test")) {
            sender.sendMessage("\u00a7cMissing permission: banbridge.test");
            return;
        }
        if (!this.plugin.getConfigManager().isDiscordEnabled()
                || this.plugin.getDiscordManager().getJDA() == null) {
            sender.sendMessage("\u00a7cDiscord is not connected.");
            return;
        }
        Container data = ComponentV2Builder.ban(new Punishment(
                "test-1", PunishmentType.BAN, "TestPlayer", null, null,
                "Console", null, null,
                "This is a test notification from BanBridge 2.0.", "PERMANENT",
                null, Instant.now(), null,
                this.plugin.getConfigManager().getServerName(),
                PunishmentSource.NATIVE, true, null, false));
        TextChannel channel = this.plugin.getDiscordManager()
                .getTextChannel(this.plugin.getConfigManager().getChannelOther());
        if (channel == null) {
            channel = this.plugin.getDiscordManager()
                    .getTextChannel(this.plugin.getConfigManager().getChannelBans());
        }
        if (channel != null) {
            channel.sendMessageComponents((MessageTopLevelComponent) data, new MessageTopLevelComponent[0])
                    .queue(
                            s -> sender.sendMessage("\u00a7aTest notification sent."),
                            ex -> sender.sendMessage("\u00a7cFailed to send test notification."));
        } else {
            sender.sendMessage("\u00a7cNo Discord channel configured.");
        }
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("banbridge.history")) {
            sender.sendMessage("\u00a7cMissing permission: banbridge.history");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /banbridge history <player>");
            return;
        }
        String target = args[1];
        this.plugin.getPunishmentManager().getHistory(target)
                .thenAccept(list -> {
                    sender.sendMessage("\u00a77=== History for \u00a7f" + target
                            + " \u00a77(" + list.size() + ") ===");
                    for (Punishment p : list) {
                        sender.sendMessage("\u00a77\u2022 \u00a7f" + p.getType().getDisplayName()
                                + " \u00a77by " + p.getModeratorUsername()
                                + " \u00a77- " + p.getReason()
                                + " \u00a77[" + p.getDuration() + "]");
                    }
                })
                .exceptionally(ex -> {
                    sender.sendMessage("\u00a7cFailed to load history.");
                    return null;
                });
    }

    private void handleLookup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("banbridge.history")) {
            sender.sendMessage("\u00a7cMissing permission: banbridge.history");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("\u00a7cUsage: /banbridge lookup <player>");
            return;
        }
        String target = args[1];
        this.plugin.getPunishmentManager().getActive(target).thenAccept(list -> {
            sender.sendMessage("\u00a77=== Active punishments for \u00a7f" + target
                    + " \u00a77(" + list.size() + ") ===");
            if (list.isEmpty()) {
                sender.sendMessage("\u00a77None.");
            }
            for (Punishment p : list) {
                sender.sendMessage("\u00a77\u2022 \u00a7f" + p.getType().getDisplayName()
                        + " \u00a77- " + p.getReason()
                        + " \u00a77(expires " + p.getExpiryAt() + ")");
            }
        });
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("banbridge.stats")) {
            sender.sendMessage("\u00a7cMissing permission: banbridge.stats");
            return;
        }
        String modUuid = args.length > 1 ? args[1] : null;
        if (modUuid == null && sender instanceof Player p) {
            modUuid = p.getUniqueId().toString();
        }
        if (modUuid == null) {
            sender.sendMessage("\u00a7cUsage: /banbridge stats <moderator-uuid>");
            return;
        }
        final String finalUuid = modUuid;
        this.plugin.getPunishmentManager().getByModerator(finalUuid).thenAccept(list -> {
            long bans = list.stream()
                    .filter(p -> p.getType().getCategory() == PunishmentType.Category.BAN).count();
            long mutes = list.stream()
                    .filter(p -> p.getType().getCategory() == PunishmentType.Category.CHAT).count();
            long kicks = list.stream()
                    .filter(p -> p.getType() == PunishmentType.KICK).count();
            sender.sendMessage("\u00a77=== Staff stats ===");
            sender.sendMessage("\u00a7fBans: \u00a77" + bans
                    + " \u00a7fMutes: \u00a77" + mutes
                    + " \u00a7fKicks: \u00a77" + kicks);
            sender.sendMessage("\u00a7fTotal actions: \u00a77" + list.size());
        });
    }

    private void handleQueue(CommandSender sender) {
        NotificationQueue queue = this.plugin.getNotificationQueue();
        sender.sendMessage("\u00a77=== Notification Queue ===");
        sender.sendMessage("\u00a7fPending in memory: \u00a77" + (queue != null ? queue.memorySize() : 0));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> cmds = List.of("help", "version", "reload", "status",
                    "integrations", "test", "history", "lookup", "stats", "queue");
            for (String c : cmds) {
                if (c.startsWith(args[0].toLowerCase())) {
                    completions.add(c);
                }
            }
        }
        return completions;
    }
}
