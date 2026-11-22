/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.statistics;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.database.DatabaseManager;
import com.shotdevs.banbridge.punishment.Punishment;
import com.shotdevs.banbridge.punishment.PunishmentType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class StatisticsManager {
    private final BanBridge plugin;
    private final DatabaseManager db;
    private final Map<String, StaffStat> stats = new ConcurrentHashMap<String, StaffStat>();

    public StatisticsManager(BanBridge plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public void recordAction(Punishment punishment) {
        String modUuid = punishment.hasModeratorUuid() ? punishment.getModeratorUuid().toString() : "console-unknown";
        String modName = punishment.getModeratorUsername();
        StaffStat prev = this.stats.computeIfAbsent(modUuid, k -> new StaffStat(modName));
        StaffStat updated = new StaffStat(prev.moderatorUuid, prev.moderatorUsername, prev.totalBans, prev.totalMutes, prev.totalWarnings, prev.totalKicks, prev.totalActions, Instant.now()).record(punishment.getType());
        this.stats.put(modUuid, updated);
        this.persist(updated);
    }

    public StaffStat getStats(String moderatorUuid) {
        return this.stats.get(moderatorUuid);
    }

    public Collection<StaffStat> getAllStats() {
        ArrayList<StaffStat> list = new ArrayList<StaffStat>(this.stats.values());
        list.sort((a, b) -> Long.compare(b.totalActions, a.totalActions));
        return list;
    }

    public List<StaffStat> getMostActive(int limit) {
        ArrayList<StaffStat> list = new ArrayList<StaffStat>(this.stats.values());
        list.sort((a, b) -> Long.compare(b.totalActions, a.totalActions));
        return list.subList(0, Math.min(limit, list.size()));
    }

    private void persist(StaffStat s) {
        if (this.db == null) {
            return;
        }
        try (Connection conn = this.db.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO staff_stats (moderator_uuid, moderator_username, total_bans, total_mutes, total_warnings, total_kicks, total_actions, last_action_at) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE moderator_username=?, total_bans=?, total_mutes=?, total_warnings=?, total_kicks=?, total_actions=?, last_action_at=? ON CONFLICT(moderator_uuid) DO UPDATE SET moderator_username=excluded.moderator_username, total_bans=excluded.total_bans, total_mutes=excluded.total_mutes, total_warnings=excluded.total_warnings, total_kicks=excluded.total_kicks, total_actions=excluded.total_actions, last_action_at=excluded.last_action_at");){
            ps.setString(1, s.moderatorUuid);
            ps.setString(2, s.moderatorUsername);
            ps.setLong(3, s.totalBans);
            ps.setLong(4, s.totalMutes);
            ps.setLong(5, s.totalWarnings);
            ps.setLong(6, s.totalKicks);
            ps.setLong(7, s.totalActions);
            ps.setObject(8, s.lastActionAt == null ? null : Timestamp.from(s.lastActionAt));
            ps.setString(9, s.moderatorUsername);
            ps.setLong(10, s.totalBans);
            ps.setLong(11, s.totalMutes);
            ps.setLong(12, s.totalWarnings);
            ps.setLong(13, s.totalKicks);
            ps.setLong(14, s.totalActions);
            ps.setObject(15, s.lastActionAt == null ? null : Timestamp.from(s.lastActionAt));
            ps.executeUpdate();
        }
        catch (SQLException ex) {
            this.persistSQLiteFallback(s);
        }
    }

    private void persistSQLiteFallback(StaffStat s) {
        block28: {
            try (Connection conn = this.db.getConnection();){
                boolean exists;
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM staff_stats WHERE moderator_uuid = ?");){
                    ps.setString(1, s.moderatorUuid);
                    exists = ps.executeQuery().next();
                }
                if (exists) {
                    ps = conn.prepareStatement("UPDATE staff_stats SET moderator_username=?, total_bans=?, total_mutes=?, total_warnings=?, total_kicks=?, total_actions=?, last_action_at=? WHERE moderator_uuid=?");
                    try {
                        ps.setString(1, s.moderatorUsername);
                        ps.setLong(2, s.totalBans);
                        ps.setLong(3, s.totalMutes);
                        ps.setLong(4, s.totalWarnings);
                        ps.setLong(5, s.totalKicks);
                        ps.setLong(6, s.totalActions);
                        ps.setObject(7, s.lastActionAt == null ? null : Timestamp.from(s.lastActionAt));
                        ps.setString(8, s.moderatorUuid);
                        ps.executeUpdate();
                        break block28;
                    }
                    finally {
                        if (ps != null) {
                            ps.close();
                        }
                    }
                }
                ps = conn.prepareStatement("INSERT INTO staff_stats (moderator_uuid, moderator_username, total_bans, total_mutes, total_warnings, total_kicks, total_actions, last_action_at) VALUES (?,?,?,?,?,?,?,?)");
                try {
                    ps.setString(1, s.moderatorUuid);
                    ps.setString(2, s.moderatorUsername);
                    ps.setLong(3, s.totalBans);
                    ps.setLong(4, s.totalMutes);
                    ps.setLong(5, s.totalWarnings);
                    ps.setLong(6, s.totalKicks);
                    ps.setLong(7, s.totalActions);
                    ps.setObject(8, s.lastActionAt == null ? null : Timestamp.from(s.lastActionAt));
                    ps.executeUpdate();
                }
                finally {
                    if (ps != null) {
                        ps.close();
                    }
                }
            }
            catch (SQLException ex) {
                this.plugin.getLogger().log(Level.WARNING, "Failed to persist staff statistics", ex);
            }
        }
    }

    public void shutdown() {
    }

    public record StaffStat(String moderatorUuid, String moderatorUsername, long totalBans, long totalMutes, long totalWarnings, long totalKicks, long totalActions, Instant lastActionAt) {
        public StaffStat(String moderatorUsername) {
            this("unknown", moderatorUsername, 0L, 0L, 0L, 0L, 0L, null);
        }

        public StaffStat record(PunishmentType type) {
            long b = this.totalBans;
            long m = this.totalMutes;
            long w = this.totalWarnings;
            long k = this.totalKicks;
            long t = this.totalActions;
            switch (type.getCategory()) {
                case BAN: {
                    ++b;
                    break;
                }
                case CHAT: {
                    if (type == PunishmentType.WARNING) {
                        ++w;
                        break;
                    }
                    ++m;
                    break;
                }
                case PLAYER: {
                    if (type == PunishmentType.KICK) {
                        ++k;
                        break;
                    }
                    ++t;
                    break;
                }
                case DISCORD: {
                    ++t;
                }
            }
            return new StaffStat(this.moderatorUuid, this.moderatorUsername, b, m, w, k, ++t, this.lastActionAt);
        }
    }
}

