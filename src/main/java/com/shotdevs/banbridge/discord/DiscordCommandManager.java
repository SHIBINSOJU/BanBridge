/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.dv8tion.jda.api.entities.Guild
 *  net.dv8tion.jda.api.entities.Member
 *  net.dv8tion.jda.api.entities.User
 *  net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
 *  net.dv8tion.jda.api.hooks.ListenerAdapter
 *  net.dv8tion.jda.api.interactions.commands.OptionMapping
 *  net.dv8tion.jda.api.interactions.commands.OptionType
 *  net.dv8tion.jda.api.interactions.commands.build.Commands
 *  net.dv8tion.jda.api.interactions.commands.build.OptionData
 *  net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.CommandSender
 *  org.bukkit.plugin.Plugin
 */
package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.PunishmentRepository;
import com.shotdevs.banbridge.discord.DiscordManager;
import com.shotdevs.banbridge.discord.DiscordNotifier;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import com.shotdevs.banbridge.statistics.StatisticsManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DiscordCommandManager
extends ListenerAdapter {
    private final BanBridge plugin;
    private final ConfigManager config;
    private final PunishmentRepository repository;
    private final StatisticsManager statistics;
    private final DiscordManager discord;
    private final DiscordNotifier notifier;
    private final Set<String> staffRoleIds = new HashSet<String>();
    private final Map<String, Long> pendingConfirmations = new HashMap<String, Long>();

    public DiscordCommandManager(BanBridge plugin, DiscordManager discord, DiscordNotifier notifier, PunishmentRepository repository, StatisticsManager statistics) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.discord = discord;
        this.notifier = notifier;
        this.repository = repository;
        this.statistics = statistics;
        this.loadStaffRoles();
    }

    public void reload() {
        this.staffRoleIds.clear();
        this.loadStaffRoles();
    }

    public void shutdown() {
        this.pendingConfirmations.clear();
    }

    private void loadStaffRoles() {
        List roles = this.config.getRawConfig().getStringList("discord.staff-roles");
        this.staffRoleIds.addAll(roles);
        this.plugin.getLogger().info("Loaded " + this.staffRoleIds.size() + " Discord staff role(s)");
    }

    public void registerCommands() {
        if (!this.config.isDiscordToMinecraft()) {
            this.plugin.getLogger().info("Discord-to-Minecraft sync disabled; skipping slash command registration");
            return;
        }
        if (this.discord.getJDA() == null) {
            this.plugin.getLogger().warning("Discord not connected; cannot register slash commands");
            return;
        }
        String guildId = this.config.getGuildId();
        if (guildId == null || guildId.isBlank()) {
            this.plugin.getLogger().warning("No guild-id configured; slash commands not registered");
            return;
        }
        OptionData user = new OptionData(OptionType.USER, "target", "Discord member to moderate", true);
        OptionData reason = new OptionData(OptionType.STRING, "reason", "Moderation reason", true);
        OptionData confirm = new OptionData(OptionType.BOOLEAN, "confirm", "Confirm this moderation action", true);
        OptionData duration = new OptionData(OptionType.STRING, "duration", "Duration (e.g. 7d, 24h)", false);
        List<SlashCommandData> commands = List.of(Commands.slash((String)"mcban", (String)"Ban a Minecraft player").addOptions(new OptionData[]{user, reason, confirm}), Commands.slash((String)"mctempban", (String)"Temporarily ban a Minecraft player").addOptions(new OptionData[]{user, duration, reason, confirm}), Commands.slash((String)"mcunban", (String)"Unban a Minecraft player").addOptions(new OptionData[]{user, reason, confirm}), Commands.slash((String)"mcmute", (String)"Mute a Minecraft player").addOptions(new OptionData[]{user, reason, confirm}), Commands.slash((String)"mcunmute", (String)"Unmute a Minecraft player").addOptions(new OptionData[]{user, reason, confirm}), Commands.slash((String)"mckick", (String)"Kick a Minecraft player").addOptions(new OptionData[]{user, reason, confirm}), Commands.slash((String)"mcwarn", (String)"Warn a Minecraft player").addOptions(new OptionData[]{user, reason, confirm}), Commands.slash((String)"history", (String)"View a player's punishment history").addOptions(new OptionData[]{user}), Commands.slash((String)"punishment", (String)"View punishment details by ID").addOptions(new OptionData[]{new OptionData(OptionType.STRING, "id", "Punishment ID", true)}), Commands.slash((String)"staffstats", (String)"View staff moderation statistics").addOptions(new OptionData[]{user}));
        Guild guild = this.discord.getJDA().getGuildById(guildId);
        if (guild == null) {
            this.plugin.getLogger().warning("Guild not found for slash command registration");
            return;
        }
        guild.updateCommands().addCommands(commands).queue(success -> this.plugin.getLogger().info("Registered " + commands.size() + " Discord slash commands"), ex -> this.plugin.getLogger().log(Level.SEVERE, "Failed to register slash commands", (Throwable)ex));
    }

    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        Member member = event.getMember();
        if (!this.isAuthorized(member)) {
            event.reply("You are not authorized to use BanBridge moderation commands.").setEphemeral(true).queue();
            return;
        }
        switch (name) {
            case "mcban": {
                this.handleBan(event, false);
                break;
            }
            case "mctempban": {
                this.handleBan(event, true);
                break;
            }
            case "mcunban": {
                this.handleUnban(event);
                break;
            }
            case "mcmute": {
                this.handleMute(event, false);
                break;
            }
            case "mcunmute": {
                this.handleMute(event, true);
                break;
            }
            case "mckick": {
                this.handleKick(event);
                break;
            }
            case "mcwarn": {
                this.handleWarn(event);
                break;
            }
            case "history": {
                this.handleHistory(event);
                break;
            }
            case "punishment": {
                this.handlePunishment(event);
                break;
            }
            case "staffstats": {
                this.handleStaffStats(event);
            }
        }
    }

    private boolean isAuthorized(Member member) {
        if (member == null) {
            return false;
        }
        if (this.staffRoleIds.isEmpty()) {
            return false;
        }
        return member.getRoles().stream().anyMatch(r -> this.staffRoleIds.contains(r.getId()));
    }

    private Optional<OfflinePlayer> resolveTarget(SlashCommandInteractionEvent event, User targetUser) {
        String username;
        String mcUuid = this.lookupLinkedMinecraft(targetUser.getId());
        if (mcUuid != null) {
            return Optional.of(Bukkit.getOfflinePlayer((UUID)UUID.fromString(mcUuid)));
        }
        String string = username = event.getOption("target") == null ? null : event.getOption("target").getAsString();
        if (username != null && !username.isBlank()) {
            return Optional.of(Bukkit.getOfflinePlayer((String)username));
        }
        event.reply("Target Discord user has not linked a Minecraft account. Link an account first.").setEphemeral(true).queue();
        return Optional.empty();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String lookupLinkedMinecraft(String discordId) {
        try (Connection conn = this.plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT minecraft_uuid FROM discord_links WHERE discord_id = ?");){
            ps.setString(1, discordId);
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return null;
                String string = rs.getString("minecraft_uuid");
                return string;
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to look up linked Minecraft account", ex);
        }
        return null;
    }

    private boolean requiresConfirmation(String command) {
        return switch (command) {
            case "mcban", "mctempban", "mcunban", "mcmute", "mcunmute", "mckick", "mcwarn" -> true;
            default -> false;
        };
    }

    private boolean isConfirmed(SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("confirm");
        return opt != null && opt.getAsBoolean();
    }

    private void handleBan(SlashCommandInteractionEvent event, boolean temporary) {
        if (!this.isConfirmed(event)) {
            event.reply("\u26a0\ufe0f This is a destructive action. Re-run with `confirm: true` to execute.").setEphemeral(true).queue();
            return;
        }
        User target = event.getOption("target").getAsUser();
        String reason = this.optString(event, "reason", "No reason provided");
        String duration = temporary ? this.optString(event, "duration", "7d") : null;
        this.resolveTarget(event, target).ifPresent(offline -> {
            String mcName = offline.getName();
            String cmd = temporary ? "tempban " + mcName + " " + duration + " " + reason : "ban " + mcName + " " + reason;
            this.executeOnServerThread(event, cmd, () -> {
                this.recordDiscordInitiated(event, target, temporary ? PunishmentType.DISCORD_TEMP_BAN : PunishmentType.DISCORD_BAN, mcName, reason, duration);
                event.reply("\u2705 " + mcName + " has been " + (temporary ? "temporarily banned" : "banned") + ".").setEphemeral(true).queue();
            }, "Failed to ban " + mcName);
        });
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        if (!this.isConfirmed(event)) {
            event.reply("\u26a0\ufe0f Re-run with `confirm: true` to execute.").setEphemeral(true).queue();
            return;
        }
        User target = event.getOption("target").getAsUser();
        String reason = this.optString(event, "reason", "No reason provided");
        this.resolveTarget(event, target).ifPresent(offline -> {
            String mcName = offline.getName();
            this.executeOnServerThread(event, "pardon " + mcName, () -> {
                this.recordDiscordInitiated(event, target, PunishmentType.DISCORD_UNBAN, mcName, reason, null);
                event.reply("\u2705 " + mcName + " has been unbanned.").setEphemeral(true).queue();
            }, "Failed to unban " + mcName);
        });
    }

    private void handleMute(SlashCommandInteractionEvent event, boolean unmute) {
        if (!this.isConfirmed(event)) {
            event.reply("\u26a0\ufe0f Re-run with `confirm: true` to execute.").setEphemeral(true).queue();
            return;
        }
        User target = event.getOption("target").getAsUser();
        String reason = this.optString(event, "reason", "No reason provided");
        this.resolveTarget(event, target).ifPresent(offline -> {
            String mcName = offline.getName();
            String cmd = unmute ? "unmute " + mcName : "mute " + mcName + " " + reason;
            this.executeOnServerThread(event, cmd, () -> {
                this.recordDiscordInitiated(event, target, unmute ? PunishmentType.DISCORD_TIMEOUT_REMOVAL : PunishmentType.DISCORD_TIMEOUT, mcName, reason, null);
                event.reply("\u2705 " + mcName + " has been " + (unmute ? "unmuted" : "muted") + ".").setEphemeral(true).queue();
            }, "Failed to " + (unmute ? "unmute" : "mute") + " " + mcName);
        });
    }

    private void handleKick(SlashCommandInteractionEvent event) {
        if (!this.isConfirmed(event)) {
            event.reply("\u26a0\ufe0f Re-run with `confirm: true` to execute.").setEphemeral(true).queue();
            return;
        }
        User target = event.getOption("target").getAsUser();
        String reason = this.optString(event, "reason", "No reason provided");
        this.resolveTarget(event, target).ifPresent(offline -> {
            String mcName = offline.getName();
            this.executeOnServerThread(event, "kick " + mcName + " " + reason, () -> {
                this.recordDiscordInitiated(event, target, PunishmentType.DISCORD_KICK, mcName, reason, null);
                event.reply("\u2705 " + mcName + " has been kicked.").setEphemeral(true).queue();
            }, "Failed to kick " + mcName);
        });
    }

    private void handleWarn(SlashCommandInteractionEvent event) {
        if (!this.isConfirmed(event)) {
            event.reply("\u26a0\ufe0f Re-run with `confirm: true` to execute.").setEphemeral(true).queue();
            return;
        }
        User target = event.getOption("target").getAsUser();
        String reason = this.optString(event, "reason", "No reason provided");
        this.resolveTarget(event, target).ifPresent(offline -> {
            String mcName = offline.getName();
            this.executeOnServerThread(event, "warn " + mcName + " " + reason, () -> {
                this.recordDiscordInitiated(event, target, PunishmentType.DISCORD_WARNING, mcName, reason, null);
                event.reply("\u2705 " + mcName + " has been warned.").setEphemeral(true).queue();
            }, "Failed to warn " + mcName);
        });
    }

    private void handleHistory(SlashCommandInteractionEvent event) {
        User target = event.getOption("target").getAsUser();
        this.resolveTarget(event, target).ifPresent(offline -> this.plugin.getPunishmentManager().getHistory(offline.getName()).thenAccept(list -> {
            StringBuilder sb = new StringBuilder("**Punishment history for " + offline.getName() + "**\n");
            if (list.isEmpty()) {
                sb.append("No recorded punishments.");
            }
            for (Punishment p : list) {
                sb.append("\u2022 ").append(p.getType().getDisplayName()).append(" | ").append(p.getModeratorUsername()).append(" | ").append(p.getReason()).append(" | ").append(p.getDuration()).append("\n");
            }
            event.reply(sb.toString()).setEphemeral(true).queue();
        }));
    }

    private void handlePunishment(SlashCommandInteractionEvent event) {
        String id = event.getOption("id").getAsString();
        this.plugin.getPunishmentManager().getPunishment(id).thenAccept(opt -> {
            if (opt.isEmpty()) {
                event.reply("Punishment not found: " + id).setEphemeral(true).queue();
                return;
            }
            Punishment p = (Punishment)opt.get();
            event.reply("**Punishment** `" + p.getId() + "`\nType: " + p.getType().getDisplayName() + "\nTarget: " + p.getTargetUsername() + "\nModerator: " + p.getModeratorUsername() + "\nReason: " + p.getReason() + "\nDuration: " + p.getDuration() + "\nActive: " + p.isActive()).setEphemeral(true).queue();
        });
    }

    private void handleStaffStats(SlashCommandInteractionEvent event) {
        User target = event.getOption("target").getAsUser();
        String mcUuid = this.lookupLinkedMinecraft(target.getId());
        if (mcUuid == null) {
            event.reply("That user has not linked a Minecraft account.").setEphemeral(true).queue();
            return;
        }
        this.plugin.getPunishmentManager().getByModerator(mcUuid).thenAccept(list -> {
            long bans = list.stream().filter(p -> p.getType().getCategory() == PunishmentType.Category.BAN).count();
            long mutes = list.stream().filter(p -> p.getType().getCategory() == PunishmentType.Category.CHAT).count();
            long kicks = list.stream().filter(p -> p.getType() == PunishmentType.KICK).count();
            event.reply("**Staff stats for " + target.getName() + "**\nBans: " + bans + "\nMutes/Chat: " + mutes + "\nKicks: " + kicks + "\nTotal actions: " + list.size()).setEphemeral(true).queue();
        });
    }

    private void executeOnServerThread(SlashCommandInteractionEvent event, String command, Runnable onSuccess, String failureMessage) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            boolean result = Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)command);
            if (result) {
                onSuccess.run();
            } else {
                event.reply("\u274c " + failureMessage).setEphemeral(true).queue();
            }
        });
    }

    private void recordDiscordInitiated(SlashCommandInteractionEvent event, User target, PunishmentType type, String mcName, String reason, String duration) {
        String linked;
        Instant now = Instant.now();
        String discordId = target.getId();
        String linkedUuid = this.lookupLinkedMinecraft(discordId);
        UUID uuid = linkedUuid == null ? null : UUID.fromString(linkedUuid);
        String moderator = event.getMember() != null ? event.getMember().getUser().getName() : "Discord Staff";
        UUID moderatorUuid = null;
        if (event.getMember() != null && (linked = this.lookupLinkedMinecraft(event.getMember().getUser().getId())) != null) {
            try {
                moderatorUuid = UUID.fromString(linked);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        Punishment p = new Punishment("dc-" + discordId + "-" + now.getEpochSecond(), type, mcName, uuid, discordId, moderator, moderatorUuid, discordId, reason, duration == null ? "PERMANENT" : duration, this.durationSeconds(duration), now, null, this.plugin.getConfigManager().getServerName(), PunishmentSource.DISCORD, true, null, true);
        this.plugin.getPunishmentManager().record(p);
        this.audit("DISCORD_TO_MC", moderator, type.name(), mcName, reason, duration);
    }

    private Long durationSeconds(String duration) {
        if (duration == null) {
            return null;
        }
        try {
            String d = duration.toLowerCase().trim();
            long val = Long.parseLong(d.replaceAll("[^0-9]", ""));
            if (d.contains("d")) {
                return val * 86400L;
            }
            if (d.contains("h")) {
                return val * 3600L;
            }
            if (d.contains("m")) {
                return val * 60L;
            }
            if (d.contains("s")) {
                return val;
            }
            return val * 86400L;
        }
        catch (Exception e) {
            return null;
        }
    }

    private void audit(String action, String moderator, String type, String target, String reason, String duration) {
        this.plugin.getLogger().info("[AUDIT] " + action + " | mod=" + moderator + " | type=" + type + " | target=" + target + " | reason=" + reason + " | duration=" + duration);
    }

    private String optString(SlashCommandInteractionEvent event, String name, String def) {
        OptionMapping opt = event.getOption(name);
        return opt == null ? def : opt.getAsString();
    }
}

