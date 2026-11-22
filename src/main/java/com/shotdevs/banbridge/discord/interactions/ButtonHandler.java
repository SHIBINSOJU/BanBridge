/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
 *  net.dv8tion.jda.api.hooks.ListenerAdapter
 */
package com.shotdevs.banbridge.discord.interactions;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.punishment.Punishment;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ButtonHandler
extends ListenerAdapter {
    private final BanBridge plugin;

    public ButtonHandler(BanBridge plugin) {
        this.plugin = plugin;
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        String customId = event.getComponentId();
        event.deferReply().queue();
        if (customId.startsWith("bb:details:")) {
            String punishmentId = customId.substring("bb:details:".length());
            this.handleDetails(event, punishmentId);
        } else if (customId.startsWith("bb:history:")) {
            String target = customId.substring("bb:history:".length());
            this.handleHistory(event, target);
        } else if (customId.startsWith("bb:evidence:")) {
            String punishmentId = customId.substring("bb:evidence:".length());
            this.handleEvidence(event, punishmentId);
        } else if (customId.equals("bb:dashboard")) {
            event.getHook().sendMessage("Open the ShotDevs moderation dashboard: https://shotdevs.com/dashboard").setEphemeral(true).queue();
        } else {
            event.getHook().sendMessage("Unknown interaction.").setEphemeral(true).queue();
        }
    }

    private void handleDetails(ButtonInteractionEvent event, String punishmentId) {
        ((CompletableFuture)this.plugin.getPunishmentManager().getPunishment(punishmentId).thenAccept(opt -> {
            if (opt.isEmpty()) {
                event.getHook().sendMessage("Punishment not found: " + punishmentId).setEphemeral(true).queue();
                return;
            }
            Punishment p = (Punishment)opt.get();
            StringBuilder sb = new StringBuilder();
            sb.append("**Punishment:** `").append(p.getId()).append("`\n");
            sb.append("**Type:** ").append(p.getType().getDisplayName()).append("\n");
            sb.append("**Target:** ").append(p.getTargetUsername());
            if (p.hasTargetUuid()) {
                sb.append(" (`").append(p.getTargetUuid()).append("`)");
            }
            sb.append("\n**Moderator:** ").append(p.getModeratorUsername());
            if (p.hasModeratorUuid()) {
                sb.append(" (`").append(p.getModeratorUuid()).append("`)");
            }
            sb.append("\n**Reason:** ").append(p.getReason());
            sb.append("\n**Duration:** ").append(p.getDuration());
            sb.append("\n**Status:** ").append(p.isActive() ? "Active" : "Revoked");
            event.getHook().sendMessage(sb.toString()).setEphemeral(true).queue();
        })).exceptionally(ex -> {
            event.getHook().sendMessage("Failed to load punishment details.").setEphemeral(true).queue();
            return null;
        });
    }

    private void handleHistory(ButtonInteractionEvent event, String target) {
        ((CompletableFuture)this.plugin.getPunishmentManager().getHistory(target).thenAccept(list -> {
            StringBuilder sb = new StringBuilder("**Punishment history for " + target + "**\n");
            if (list.isEmpty()) {
                sb.append("No recorded punishments.");
            } else {
                for (Punishment p : list) {
                    sb.append("\u2022 ").append(p.getType().getDisplayName()).append(" | ").append(p.getModeratorUsername()).append(" | ").append(p.getReason()).append(" | ").append(p.getDuration()).append("\n");
                }
            }
            event.getHook().sendMessage(sb.toString()).setEphemeral(true).queue();
        })).exceptionally(ex -> {
            event.getHook().sendMessage("Failed to load history.").setEphemeral(true).queue();
            return null;
        });
    }

    private void handleEvidence(ButtonInteractionEvent event, String punishmentId) {
        ((CompletableFuture)this.plugin.getPunishmentManager().getPunishment(punishmentId).thenAccept(opt -> {
            if (opt.isEmpty() || !((Punishment)opt.get()).hasEvidence()) {
                event.getHook().sendMessage("No evidence attached to this punishment.").setEphemeral(true).queue();
                return;
            }
            event.getHook().sendMessage("**Evidence:** " + ((Punishment)opt.get()).getEvidenceUrl()).setEphemeral(true).queue();
        })).exceptionally(ex -> {
            event.getHook().sendMessage("Failed to load evidence.").setEphemeral(true).queue();
            return null;
        });
    }
}

