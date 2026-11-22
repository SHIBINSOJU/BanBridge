/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.integrations.libertybans;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.integrations.AbstractIntegration;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class LibertyBansIntegration
extends AbstractIntegration {
    public LibertyBansIntegration(BanBridge plugin) {
        super(plugin, "LibertyBans", "libertybans");
    }

    @Override
    public String name() {
        return "LibertyBans";
    }

    @Override
    public List<PunishmentType> getSupportedTypes() {
        return List.of(PunishmentType.BAN, PunishmentType.TEMP_BAN, PunishmentType.IP_BAN, PunishmentType.UNBAN, PunishmentType.PERM_MUTE, PunishmentType.TEMP_MUTE, PunishmentType.UNMUTE, PunishmentType.WARNING, PunishmentType.KICK, PunishmentType.JAIL, PunishmentType.UNJAIL);
    }

    @Override
    public Optional<Punishment> toPunishment(Object rawEvent) {
        if (rawEvent instanceof Map) {
            Map map;
            Map data = map = (Map)rawEvent;
            return this.fromData(data, PunishmentSource.LIBERTYBANS);
        }
        this.plugin.getLogger().log(Level.FINE, "LibertyBans event type not mapped: " + (rawEvent == null ? "null" : rawEvent.getClass().getName()));
        return Optional.empty();
    }
}

