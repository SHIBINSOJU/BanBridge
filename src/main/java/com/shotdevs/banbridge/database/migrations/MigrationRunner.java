/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MigrationRunner {
    public static void run(Connection conn) throws SQLException {
        MigrationRunner.createTables(conn);
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();){
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS punishments (\n    id TEXT PRIMARY KEY,\n    type TEXT NOT NULL,\n    target_username TEXT NOT NULL,\n    target_uuid TEXT,\n    target_discord_id TEXT,\n    moderator_username TEXT NOT NULL,\n    moderator_uuid TEXT,\n    moderator_discord_id TEXT,\n    reason TEXT NOT NULL,\n    duration TEXT NOT NULL,\n    duration_seconds INTEGER,\n    issued_at TEXT,\n    expiry_at TEXT,\n    server_name TEXT NOT NULL,\n    source TEXT NOT NULL,\n    active INTEGER NOT NULL DEFAULT 1,\n    evidence_url TEXT,\n    discord_notified INTEGER NOT NULL DEFAULT 0,\n    created_at TEXT DEFAULT CURRENT_TIMESTAMP\n);\n");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_target ON punishments(target_username);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_type ON punishments(type);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_active ON punishments(active);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_moderator ON punishments(moderator_uuid);");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_punishments_issued ON punishments(issued_at);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS notification_queue (\n    id INTEGER PRIMARY KEY AUTOINCREMENT,\n    payload_key TEXT NOT NULL UNIQUE,\n    created_at TEXT NOT NULL,\n    attempts INTEGER NOT NULL DEFAULT 0,\n    delivered INTEGER NOT NULL DEFAULT 0\n);\n");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_queue_delivered ON notification_queue(delivered, attempts);");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS staff_stats (\n    moderator_uuid TEXT PRIMARY KEY,\n    moderator_username TEXT NOT NULL,\n    total_bans INTEGER DEFAULT 0,\n    total_mutes INTEGER DEFAULT 0,\n    total_warnings INTEGER DEFAULT 0,\n    total_kicks INTEGER DEFAULT 0,\n    total_actions INTEGER DEFAULT 0,\n    last_action_at TEXT\n);\n");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS discord_links (\n    discord_id TEXT PRIMARY KEY,\n    minecraft_uuid TEXT NOT NULL,\n    minecraft_username TEXT NOT NULL,\n    linked_at TEXT NOT NULL\n);\n");
        }
    }
}

