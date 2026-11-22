package com.shotdevs.banbridge.punishment;

public enum PunishmentType {
    BAN("Ban", "ban"),
    TEMP_BAN("Temporary Ban", "tempban"),
    IP_BAN("IP Ban", "ipban"),
    TEMP_IP_BAN("Temporary IP Ban", "tempipban"),
    UNBAN("Unban", "unban"),
    IP_UNBAN("IP Unban", "ipunban"),
    PERM_MUTE("Permanent Mute", "mute"),
    TEMP_MUTE("Temporary Mute", "tempmute"),
    UNMUTE("Unmute", "unmute"),
    CHAT_RESTRICTION("Chat Restriction", "chatrestriction"),
    WARNING("Warning", "warn"),
    WARNING_REMOVAL("Warning Removal", "warnremove"),
    WARNING_ESCALATION("Warning Escalation", "warnescalation"),
    KICK("Kick", "kick"),
    FREEZE("Freeze", "freeze"),
    UNFREEZE("Unfreeze", "unfreeze"),
    JAIL("Jail", "jail"),
    UNJAIL("Unjail", "unjail"),
    PLAYER_LOCKDOWN("Player Lockdown", "lockdown"),
    COMBAT_RESTRICTION("Combat Restriction", "combatarestriction"),
    DISCORD_BAN("Discord Ban", "discordban"),
    DISCORD_TEMP_BAN("Discord Temporary Ban", "discordtempban"),
    DISCORD_UNBAN("Discord Unban", "discordunban"),
    DISCORD_KICK("Discord Kick", "discordkick"),
    DISCORD_TIMEOUT("Discord Timeout", "discordtimeout"),
    DISCORD_TIMEOUT_REMOVAL("Discord Timeout Removal", "discordtimeoutremoval"),
    DISCORD_WARNING("Discord Warning", "discordwarn"),
    DISCORD_ROLE_ACTION("Discord Role Action", "discordrole");

    private final String displayName;
    private final String commandAlias;

    PunishmentType(String displayName, String commandAlias) {
        this.displayName = displayName;
        this.commandAlias = commandAlias;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getCommandAlias() {
        return this.commandAlias;
    }

    public boolean isRevocation() {
        return this == UNBAN || this == IP_UNBAN || this == UNMUTE || this == UNFREEZE
                || this == UNJAIL || this == WARNING_REMOVAL || this == DISCORD_UNBAN
                || this == DISCORD_TIMEOUT_REMOVAL;
    }

    public boolean isDiscordAction() {
        return this == DISCORD_BAN || this == DISCORD_TEMP_BAN || this == DISCORD_UNBAN
                || this == DISCORD_KICK || this == DISCORD_TIMEOUT || this == DISCORD_TIMEOUT_REMOVAL
                || this == DISCORD_WARNING || this == DISCORD_ROLE_ACTION;
    }

    public boolean hasDuration() {
        return this == TEMP_BAN || this == TEMP_IP_BAN || this == TEMP_MUTE
                || this == DISCORD_TEMP_BAN || this == DISCORD_TIMEOUT;
    }

    public boolean isPermanent() {
        return this == BAN || this == IP_BAN || this == PERM_MUTE;
    }

    public Category getCategory() {
        // ordinals 0-5: BAN, TEMP_BAN, IP_BAN, TEMP_IP_BAN, UNBAN, IP_UNBAN
        // ordinals 6-12: PERM_MUTE, TEMP_MUTE, UNMUTE, CHAT_RESTRICTION, WARNING, WARNING_REMOVAL, WARNING_ESCALATION
        // ordinals 13-19: KICK, FREEZE, UNFREEZE, JAIL, UNJAIL, PLAYER_LOCKDOWN, COMBAT_RESTRICTION
        // ordinals 20-27: DISCORD_BAN ... DISCORD_ROLE_ACTION
        int ord = this.ordinal();
        if (ord <= 5) return Category.BAN;
        if (ord <= 12) return Category.CHAT;
        if (ord <= 19) return Category.PLAYER;
        return Category.DISCORD;
    }

    public enum Category {
        BAN,
        CHAT,
        PLAYER,
        DISCORD
    }
}
