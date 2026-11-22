/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.integrations;

import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.util.List;
import java.util.Optional;

public interface Integration {
    public String name();

    public boolean isInstalled();

    public String getVersion();

    public boolean isEnabled();

    public void setEnabled(boolean var1);

    public List<PunishmentType> getSupportedTypes();

    public Optional<Punishment> toPunishment(Object var1);

    public void enable();

    public void disable();
}

