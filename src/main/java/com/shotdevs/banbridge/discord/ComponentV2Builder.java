package com.shotdevs.banbridge.discord;

import com.shotdevs.banbridge.punishment.Punishment;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ComponentV2Builder {
    private static final String FOOTER_PREFIX = "*BanBridge | ShotDevs Moderation System*";
    private static final String BRAND_ICON = "https://cdn.discordapp.com/emojis/1143919872249790515.png";

    private ComponentV2Builder() {}

    private static TextDisplay text(String content) {
        return TextDisplay.of(content);
    }

    private static Separator sep(Separator.Spacing spacing) {
        return Separator.createDivider(spacing);
    }

    private static String instantOrUnknown(Instant i) {
        if (i == null) return "UNKNOWN";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault()).format(i);
    }

    private static String discordTs(Instant i) {
        if (i == null) return "UNKNOWN";
        return "<t:" + i.getEpochSecond() + ":F>";
    }

    private static String discordTsRelative(Instant i) {
        if (i == null) return "UNKNOWN";
        return "<t:" + i.getEpochSecond() + ":R>";
    }

    private static String duration(Punishment p) {
        String d = p.getDuration();
        if (p.getType().isPermanent()) return "PERMANENT";
        if (d == null || d.isBlank() || d.equalsIgnoreCase("UNKNOWN")) return "UNKNOWN";
        return d;
    }

    private static String expiry(Punishment p) {
        if (p.getExpiryAt() == null) {
            return p.getType().isPermanent() ? "PERMANENT" : "UNKNOWN";
        }
        return instantOrUnknown(p.getExpiryAt()) + " (" + discordTsRelative(p.getExpiryAt()) + ")";
    }

    private static List<ContainerChildComponent> baseComponents(
            String header, List<TextDisplay> fields, boolean showButtons, Punishment p) {

        List<ContainerChildComponent> comps = new ArrayList<>();
        comps.add(text("### " + header));
        comps.add(sep(Separator.Spacing.LARGE));

        for (TextDisplay f : fields) {
            comps.add(f);
        }

        comps.add(sep(Separator.Spacing.SMALL));

        // Brand thumbnail section
        Section brandSection = Section.of(
                (SectionAccessoryComponent) Thumbnail.fromUrl(BRAND_ICON),
                (SectionContentComponent) text("\u200b")
        );
        comps.add(brandSection);

        comps.add(sep(Separator.Spacing.LARGE));
        comps.add(text(FOOTER_PREFIX));

        if (showButtons && p != null) {
            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.secondary("bb:details:" + p.getId(), "View Details"));
            buttons.add(Button.secondary(
                    "bb:history:" + (p.hasTargetUuid() ? p.getTargetUuid() : "unknown"),
                    "View History"));
            if (p.hasEvidence()) {
                buttons.add(Button.secondary("bb:evidence:" + p.getId(), "View Evidence"));
            }
            buttons.add(Button.link("https://shotdevs.com/dashboard", "Open Dashboard"));

            // Build ActionRow from list
            List<ActionRowChildComponent> rowItems = new ArrayList<>();
            for (Button b : buttons) {
                rowItems.add(b);
            }
            comps.add(ActionRow.of(rowItems));
        }

        return comps;
    }

    private static Container container(List<ContainerChildComponent> comps) {
        return Container.of(comps);
    }

    public static Container ban(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Punished by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())
                + "\n**Duration:** " + duration(p)
                + "\n**Expiry:** " + expiry(p)));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Source:** " + p.getSource().getLabel()
                + "\n**Date:** " + instantOrUnknown(p.getIssuedAt())
                + "\n**Time:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDD28 PLAYER BANNED", fields, true, p));
    }

    public static Container mute(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Muted by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())
                + "\n**Duration:** " + duration(p)
                + "\n**Expiry:** " + expiry(p)));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDD07 PLAYER MUTED", fields, true, p));
    }

    public static Container warning(Punishment p, int totalActiveWarnings) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Warned by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())
                + "\n**Warning #:** 1"
                + "\n**Total active warnings:** " + totalActiveWarnings));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\u26A0\uFE0F PLAYER WARNED", fields, true, p));
    }

    public static Container kick(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Kicked by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDC62 PLAYER KICKED", fields, true, p));
    }

    public static Container freeze(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Frozen by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83E\uDDCA PLAYER FROZEN", fields, true, p));
    }

    public static Container unfreeze(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Unfrozen by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83E\uDDCA PLAYER UNFROZEN", fields, true, p));
    }

    public static Container jail(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Jailed by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())
                + "\n**Duration:** " + duration(p)
                + "\n**Expiry:** " + expiry(p)));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDD12 PLAYER JAILED", fields, true, p));
    }

    public static Container unjail(Punishment p) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Unjailed by:** " + escape(p.getModeratorUsername())
                + "\n**Reason:** " + escape(p.getReason())));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDD13 PLAYER UNJAILED", fields, true, p));
    }

    public static Container unban(Punishment p, String previousReason, String previousDuration) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Unbanned by:** " + escape(p.getModeratorUsername())
                + "\n**Previous reason:** " + escape(previousReason)
                + "\n**Previous duration:** " + (previousDuration == null ? "UNKNOWN" : previousDuration)));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDFE2 PLAYER UNBANNED", fields, true, p));
    }

    public static Container unmute(Punishment p, String previousReason, String previousDuration) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**Player:** " + escape(p.getTargetUsername())
                + (p.hasTargetUuid() ? "\n**UUID:** `" + p.getTargetUuid() + "`" : "")));
        fields.add(text("**Unmuted by:** " + escape(p.getModeratorUsername())
                + "\n**Previous mute reason:** " + escape(previousReason)
                + "\n**Previous duration:** " + (previousDuration == null ? "UNKNOWN" : previousDuration)));
        fields.add(text("**Server:** " + escape(p.getServerName())
                + "\n**Timestamp:** " + discordTs(p.getIssuedAt())));
        return container(baseComponents("\uD83D\uDD0A PLAYER UNMUTED", fields, true, p));
    }

    public static Container discordBan(String discordUser, String userId, String moderator,
                                        String reason, String guildName, Instant issuedAt) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**User:** " + escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(text("**Moderator:** " + escape(moderator)
                + "\n**Reason:** " + escape(reason) + "\n**Guild:** " + escape(guildName)));
        fields.add(text("**Timestamp:** " + discordTs(issuedAt)));
        return container(baseComponents("\uD83D\uDD28 DISCORD USER BANNED", fields, false, null));
    }

    public static Container discordUnban(String discordUser, String userId, String moderator,
                                          String guildName, Instant issuedAt) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**User:** " + escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(text("**Moderator:** " + escape(moderator) + "\n**Guild:** " + escape(guildName)));
        fields.add(text("**Timestamp:** " + discordTs(issuedAt)));
        return container(baseComponents("\uD83D\uDFE2 DISCORD USER UNBANNED", fields, false, null));
    }

    public static Container discordKick(String discordUser, String userId, String moderator,
                                         String reason, String guildName, Instant issuedAt) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**User:** " + escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(text("**Moderator:** " + escape(moderator)
                + "\n**Reason:** " + escape(reason) + "\n**Guild:** " + escape(guildName)));
        fields.add(text("**Timestamp:** " + discordTs(issuedAt)));
        return container(baseComponents("\uD83D\uDC62 DISCORD USER KICKED", fields, false, null));
    }

    public static Container discordTimeout(String discordUser, String userId, String moderator,
                                            String reason, String duration, Instant expiry,
                                            String guildName, Instant issuedAt) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**User:** " + escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(text("**Moderator:** " + escape(moderator)
                + "\n**Reason:** " + escape(reason)
                + "\n**Duration:** " + (duration == null ? "UNKNOWN" : duration)
                + "\n**Expiry:** " + (expiry == null ? "UNKNOWN" : discordTs(expiry))
                + "\n**Guild:** " + escape(guildName)));
        fields.add(text("**Timestamp:** " + discordTs(issuedAt)));
        return container(baseComponents("\u23F1\uFE0F DISCORD USER TIMED OUT", fields, false, null));
    }

    public static Container discordTimeoutRemoval(String discordUser, String userId, String moderator,
                                                   String guildName, Instant issuedAt) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**User:** " + escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(text("**Moderator:** " + escape(moderator) + "\n**Guild:** " + escape(guildName)));
        fields.add(text("**Timestamp:** " + discordTs(issuedAt)));
        return container(baseComponents("\u23F1\uFE0F DISCORD TIMEOUT REMOVED", fields, false, null));
    }

    public static Container discordWarning(String discordUser, String userId, String moderator,
                                            String reason, int warningNumber, String guildName, Instant issuedAt) {
        List<TextDisplay> fields = new ArrayList<>();
        fields.add(text("**User:** " + escape(discordUser) + "\n**User ID:** `" + userId + "`"));
        fields.add(text("**Moderator:** " + escape(moderator)
                + "\n**Reason:** " + escape(reason)
                + "\n**Warning #:** " + warningNumber));
        fields.add(text("**Guild:** " + escape(guildName)
                + "\n**Timestamp:** " + discordTs(issuedAt)));
        return container(baseComponents("\u26A0\uFE0F DISCORD USER WARNED", fields, false, null));
    }

    private static String escape(String s) {
        if (s == null) return "UNKNOWN";
        return s.replace("@", "@\u200b");
    }
}
