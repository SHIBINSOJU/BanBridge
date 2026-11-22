/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.utilities;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeFormat {
    private static final Pattern DURATION = Pattern.compile("^(?:(\\d+)\\s*d)?(?:(\\d+)\\s*h)?(?:(\\d+)\\s*m)?(?:(\\d+)\\s*s)?$", 2);

    public static Long parseSeconds(String duration) {
        if (duration == null) {
            return null;
        }
        String d = duration.trim().toLowerCase(Locale.ROOT);
        if (d.isEmpty() || d.equals("permanent") || d.equals("perm") || d.equals("0")) {
            return null;
        }
        if (d.equalsIgnoreCase("unknown")) {
            return null;
        }
        Matcher m = DURATION.matcher(d);
        if (!m.matches()) {
            return null;
        }
        long seconds = 0L;
        if (m.group(1) != null) {
            seconds += Long.parseLong(m.group(1)) * 86400L;
        }
        if (m.group(2) != null) {
            seconds += Long.parseLong(m.group(2)) * 3600L;
        }
        if (m.group(3) != null) {
            seconds += Long.parseLong(m.group(3)) * 60L;
        }
        if (m.group(4) != null) {
            seconds += Long.parseLong(m.group(4));
        }
        return seconds > 0L ? Long.valueOf(seconds) : null;
    }

    public static String formatSeconds(Long seconds) {
        if (seconds == null) {
            return "UNKNOWN";
        }
        if (seconds <= 0L) {
            return "PERMANENT";
        }
        long d = seconds / 86400L;
        long h = seconds % 86400L / 3600L;
        long m = seconds % 3600L / 60L;
        long s = seconds % 60L;
        StringBuilder sb = new StringBuilder();
        if (d > 0L) {
            sb.append(d).append("d ");
        }
        if (h > 0L) {
            sb.append(h).append("h ");
        }
        if (m > 0L) {
            sb.append(m).append("m ");
        }
        if (s > 0L || sb.isEmpty()) {
            sb.append(s).append("s");
        }
        return sb.toString().trim();
    }

    public static String formatInstant(Instant instant, ZoneId zone) {
        if (instant == null) {
            return "UNKNOWN";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(zone).format(instant);
    }

    public static boolean isExpired(Instant expiry) {
        return expiry != null && Instant.now().isAfter(expiry);
    }
}

