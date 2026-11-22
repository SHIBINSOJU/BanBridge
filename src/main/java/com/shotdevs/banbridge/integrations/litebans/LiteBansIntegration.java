/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.integrations.litebans;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.integrations.AbstractIntegration;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentSource;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class LiteBansIntegration
extends AbstractIntegration {
    public LiteBansIntegration(BanBridge plugin) {
        super(plugin, "LiteBans", "litebans");
    }

    @Override
    public String name() {
        return "LiteBans";
    }

    @Override
    public List<PunishmentType> getSupportedTypes() {
        return List.of(PunishmentType.BAN, PunishmentType.TEMP_BAN, PunishmentType.IP_BAN, PunishmentType.TEMP_IP_BAN, PunishmentType.UNBAN, PunishmentType.IP_UNBAN, PunishmentType.PERM_MUTE, PunishmentType.TEMP_MUTE, PunishmentType.UNMUTE, PunishmentType.WARNING, PunishmentType.KICK, PunishmentType.FREEZE, PunishmentType.UNFREEZE, PunishmentType.JAIL, PunishmentType.UNJAIL);
    }

    @Override
    public Optional<Punishment> toPunishment(Object rawEvent) {
        if (rawEvent instanceof Map) {
            Map map;
            Map data = map = (Map)rawEvent;
            return this.fromData(data, PunishmentSource.LITEBANS);
        }
        this.plugin.getLogger().log(Level.FINE, "LiteBans event type not mapped: " + (rawEvent == null ? "null" : rawEvent.getClass().getName()));
        return Optional.empty();
    }
}

