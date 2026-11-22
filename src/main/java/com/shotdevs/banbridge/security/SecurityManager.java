/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.security;

import com.shotdevs.banbridge.BanBridge;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

public class SecurityManager {
    private static final Pattern DISCORD_ID = Pattern.compile("^[0-9]{17,19}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final int MAX_REASON_LENGTH = 1024;
    private static final int MAX_USERNAME_LENGTH = 32;
    private final BanBridge plugin;
    private final Map<String, Deque<Long>> actionTimes = new ConcurrentHashMap<String, Deque<Long>>();
    private static final int MAX_ACTIONS_PER_MINUTE = 30;

    public SecurityManager(BanBridge plugin) {
        this.plugin = plugin;
    }

    public boolean isValidUuid(String value) {
        return value != null && UUID_PATTERN.matcher(value).matches();
    }

    public boolean isValidDiscordId(String value) {
        return value != null && DISCORD_ID.matcher(value).matches();
    }

    public boolean isValidUsername(String value) {
        return value != null && !value.isBlank() && value.length() <= 32 && value.matches("^[a-zA-Z0-9_]+$");
    }

    public String sanitizeReason(String reason) {
        if (reason == null) {
            return "No reason provided";
        }
        String cleaned = reason.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
        if (cleaned.isBlank()) {
            return "No reason provided";
        }
        return cleaned.length() > 1024 ? cleaned.substring(0, 1024) : cleaned;
    }

    public boolean isRateLimited(String moderatorId) {
        if (moderatorId == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        Deque times = this.actionTimes.computeIfAbsent(moderatorId, k -> new ConcurrentLinkedDeque());
        long minuteAgo = now - 60000L;
        while (!times.isEmpty() && (Long)times.peekFirst() < minuteAgo) {
            times.pollFirst();
        }
        if (times.size() >= 30) {
            this.plugin.getLogger().warning("Rate limit exceeded for moderator " + moderatorId);
            return true;
        }
        times.addLast(now);
        return false;
    }

    public void logSecurityEvent(String event, String detail) {
        this.plugin.getLogger().info("[SECURITY] " + event + " | " + detail);
    }
}

