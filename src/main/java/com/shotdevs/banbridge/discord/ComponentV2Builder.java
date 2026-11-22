/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.dv8tion.jda.api.components.actionrow.ActionRow
 *  net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
 *  net.dv8tion.jda.api.components.buttons.Button
 *  net.dv8tion.jda.api.components.container.Container
 *  net.dv8tion.jda.api.components.container.ContainerChildComponent
 *  net.dv8tion.jda.api.components.section.Section
 *  net.dv8tion.jda.api.components.section.SectionAccessoryComponent
 *  net.dv8tion.jda.api.components.section.SectionContentComponent
 *  net.dv8tion.jda.api.components.separator.Separator
 *  net.dv8tion.jda.api.components.separator.Separator$Spacing
 *  net.dv8tion.jda.api.components.textdisplay.TextDisplay
 *  net.dv8tion.jda.api.components.thumbnail.Thumbnail
 */
package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.punishment.Punishment;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent;
import net.dv8tion.jda.api.components.section.SectionContentComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;

public class ComponentV2Builder {
    private static final String FOOTER_PREFIX = "*BanBridge | ShotDevs Moderation System*";
    private static final String BRAND_ICON = "https://cdn.discordapp.com/emojis/1143919872249790515.png";

    private ComponentV2Builder() {
    }

    private static TextDisplay text(String content) {
        return TextDisplay.of((String)content);
    }

    private static Separator sep(Separator.Spacing spacing) {
        return Separator.createDivider((Separator.Spacing)spacing);
    }

    private static String instantOrUnknown(Instant i) {
        if (i == null) {
            return "UNKNOWN";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(i);
    }

    private static String discordTs(Instant i) {
        if (i == null) {
            return "UNKNOWN";
        }
        return "<t:" + i.getEpochSecond() + ":F>";
    }

    private static String discordTsRelative(Instant i) {
        if (i == null) {
            return "UNKNOWN";
        }
        return "<t:" + i.getEpochSecond() + ":R>";
    }

    private static String duration(Punishment p) {
        String d = p.getDuration();
        if (p.getType().isPermanent()) {
            return "PERMANENT";
        }
        if (d == null || d.isBlank() || d.equalsIgnoreCase("UNKNOWN")) {
            return "UNKNOWN";
        }
        return d;
    }

    private static String expiry(Punishment p) {
        if (p.getExpiryAt() == null) {
            if (p.getType().isPermanent()) {
                return "PERMANENT";
            }
            return "UNKNOWN";
        }
        return ComponentV2Builder.instantOrUnknown(p.getExpiryAt()) + " (" + ComponentV2Builder.discordTsRelative(p.getExpiryAt()) + ")";
    }

    private static List<ContainerChildComponent> baseComponents(String header, List<TextDisplay> fields, boolean showButtons, Punishment p) {
        ArrayList<ContainerChildComponent> comps = new ArrayList<ContainerChildComponent>();
        comps.add((ContainerChildComponent)ComponentV2Builder.text("### " + header));
        comps.add((ContainerChildComponent)ComponentV2Builder.sep(Separator.Spacing.LARGE));
        for (TextDisplay f : fields) {
            comps.add((ContainerChildComponent)f);
        }
        comps.add((ContainerChildComponent)ComponentV2Builder.sep(Separator.Spacing.SMALL));
        comps.add((ContainerChildComponent)Section.of((SectionAccessoryComponent)Thumbnail.fromUrl((String)BRAND_ICON), (SectionContentComponent)ComponentV2Builder.text("\u200b"), (SectionContentComponent[])new SectionContentComponent[0]));
        comps.add((ContainerChildComponent)ComponentV2Builder.sep(Separator.Spacing.LARGE));
        comps.add((ContainerChildComponent)ComponentV2Builder.text(FOOTER_PREFIX));
        if (showButtons) {
            ArrayList<Button> buttons = new ArrayList<Button>();
            buttons.add(Button.secondary((String)("bb:details:" + p.getId()), (String)"View Details"));
            buttons.add(Button.secondary((String)("bb:history:" + String.valueOf(p.hasTargetUuid() ? p.getTargetUuid() : "unknown")), (String)"View History"));
            if (p.hasEvidence()) {
                buttons.add(Button.secondary((String)("bb:evidence:" + p.getId()), (String)"View Evidence"));
            }
            buttons.add(Button.link((String)"https://shotdevs.com/dashboard", (String)"Open Dashboard"));
            Button[] b = buttons.toArray(new Button[0]);
            comps.add((ContainerChildComponent)ActionRow.of((ActionRowChildComponent)b[0], (ActionRowChildComponent[])((ActionRowChildComponent[])Arrays.copyOfRange(b, 1, b.length))));
        }
        return comps;
    }

    private static Container container(ContainerChildComponent ... comps) {
        return Container.of(List.of(comps));
    }

    public static Container ban(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Punished by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason()) + "\n**Duration:** " + ComponentV2Builder.duration(p) + "\n**Expiry:** " + ComponentV2Builder.expiry(p)));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Source:** " + p.getSource().getLabel() + "\n**Date:** " + ComponentV2Builder.instantOrUnknown(p.getIssuedAt()) + "\n**Time:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udd28 PLAYER BANNED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container mute(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Muted by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason()) + "\n**Duration:** " + ComponentV2Builder.duration(p) + "\n**Expiry:** " + ComponentV2Builder.expiry(p)));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udd07 PLAYER MUTED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container warning(Punishment p, int totalActiveWarnings) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Warned by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason()) + "\n**Warning #:** 1\n**Total active warnings:** " + totalActiveWarnings));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\u26a0\ufe0f PLAYER WARNED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container kick(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Kicked by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason())));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udc62 PLAYER KICKED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container freeze(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Frozen by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason())));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83e\uddca PLAYER FROZEN", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container unfreeze(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Unfrozen by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason())));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83e\uddca PLAYER UNFROZEN", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container jail(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Jailed by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason()) + "\n**Duration:** " + ComponentV2Builder.duration(p) + "\n**Expiry:** " + ComponentV2Builder.expiry(p)));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udd12 PLAYER JAILED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container unjail(Punishment p) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Unjailed by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Reason:** " + ComponentV2Builder.escape(p.getReason())));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udd13 PLAYER UNJAILED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container unban(Punishment p, String previousReason, String previousDuration) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Unbanned by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Previous reason:** " + ComponentV2Builder.escape(previousReason) + "\n**Previous duration:** " + (previousDuration == null ? "UNKNOWN" : previousDuration)));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udfe2 PLAYER UNBANNED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container unmute(Punishment p, String previousReason, String previousDuration) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**Player:** " + ComponentV2Builder.escape(p.getTargetUsername()) + (String)(p.hasTargetUuid() ? "\n**UUID:** `" + String.valueOf(p.getTargetUuid()) + "`" : "")));
        fields.add(ComponentV2Builder.text("**Unmuted by:** " + ComponentV2Builder.escape(p.getModeratorUsername()) + "\n**Previous mute reason:** " + ComponentV2Builder.escape(previousReason) + "\n**Previous duration:** " + (previousDuration == null ? "UNKNOWN" : previousDuration)));
        fields.add(ComponentV2Builder.text("**Server:** " + ComponentV2Builder.escape(p.getServerName()) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(p.getIssuedAt())));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udd0a PLAYER UNMUTED", fields, true, p).toArray(new ContainerChildComponent[0]));
    }

    public static Container discordBan(String discordUser, String userId, String moderator, String reason, String guildName, Instant issuedAt) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**User:** " + ComponentV2Builder.escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(ComponentV2Builder.text("**Moderator:** " + ComponentV2Builder.escape(moderator) + "\n**Reason:** " + ComponentV2Builder.escape(reason) + "\n**Guild:** " + ComponentV2Builder.escape(guildName)));
        fields.add(ComponentV2Builder.text("**Timestamp:** " + ComponentV2Builder.discordTs(issuedAt)));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udd28 DISCORD USER BANNED", fields, false, null).toArray(new ContainerChildComponent[0]));
    }

    public static Container discordUnban(String discordUser, String userId, String moderator, String guildName, Instant issuedAt) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**User:** " + ComponentV2Builder.escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(ComponentV2Builder.text("**Moderator:** " + ComponentV2Builder.escape(moderator) + "\n**Guild:** " + ComponentV2Builder.escape(guildName)));
        fields.add(ComponentV2Builder.text("**Timestamp:** " + ComponentV2Builder.discordTs(issuedAt)));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udfe2 DISCORD USER UNBANNED", fields, false, null).toArray(new ContainerChildComponent[0]));
    }

    public static Container discordKick(String discordUser, String userId, String moderator, String reason, String guildName, Instant issuedAt) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**User:** " + ComponentV2Builder.escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(ComponentV2Builder.text("**Moderator:** " + ComponentV2Builder.escape(moderator) + "\n**Reason:** " + ComponentV2Builder.escape(reason) + "\n**Guild:** " + ComponentV2Builder.escape(guildName)));
        fields.add(ComponentV2Builder.text("**Timestamp:** " + ComponentV2Builder.discordTs(issuedAt)));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\ud83d\udc62 DISCORD USER KICKED", fields, false, null).toArray(new ContainerChildComponent[0]));
    }

    public static Container discordTimeout(String discordUser, String userId, String moderator, String reason, String duration, Instant expiry, String guildName, Instant issuedAt) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**User:** " + ComponentV2Builder.escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(ComponentV2Builder.text("**Moderator:** " + ComponentV2Builder.escape(moderator) + "\n**Reason:** " + ComponentV2Builder.escape(reason) + "\n**Duration:** " + (duration == null ? "UNKNOWN" : duration) + "\n**Expiry:** " + (expiry == null ? "UNKNOWN" : ComponentV2Builder.discordTs(expiry)) + "\n**Guild:** " + ComponentV2Builder.escape(guildName)));
        fields.add(ComponentV2Builder.text("**Timestamp:** " + ComponentV2Builder.discordTs(issuedAt)));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\u23f1\ufe0f DISCORD USER TIMED OUT", fields, false, null).toArray(new ContainerChildComponent[0]));
    }

    public static Container discordTimeoutRemoval(String discordUser, String userId, String moderator, String guildName, Instant issuedAt) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**User:** " + ComponentV2Builder.escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(ComponentV2Builder.text("**Moderator:** " + ComponentV2Builder.escape(moderator) + "\n**Guild:** " + ComponentV2Builder.escape(guildName)));
        fields.add(ComponentV2Builder.text("**Timestamp:** " + ComponentV2Builder.discordTs(issuedAt)));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\u23f1\ufe0f DISCORD TIMEOUT REMOVED", fields, false, null).toArray(new ContainerChildComponent[0]));
    }

    public static Container discordWarning(String discordUser, String userId, String moderator, String reason, int warningNumber, String guildName, Instant issuedAt) {
        ArrayList<TextDisplay> fields = new ArrayList<TextDisplay>();
        fields.add(ComponentV2Builder.text("**User:** " + ComponentV2Builder.escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(ComponentV2Builder.text("**Moderator:** " + ComponentV2Builder.escape(moderator) + "\n**Reason:** " + ComponentV2Builder.escape(reason) + "\n**Warning #:** " + warningNumber));
        fields.add(ComponentV2Builder.text("**Guild:** " + ComponentV2Builder.escape(guildName) + "\n**Timestamp:** " + ComponentV2Builder.discordTs(issuedAt)));
        return ComponentV2Builder.container(ComponentV2Builder.baseComponents("\u26a0\ufe0f DISCORD USER WARNED", fields, false, null).toArray(new ContainerChildComponent[0]));
    }

    private static String escape(String s) {
        if (s == null) {
            return "UNKNOWN";
        }
        return s.replace("@", "@\u200b");
    }
}

