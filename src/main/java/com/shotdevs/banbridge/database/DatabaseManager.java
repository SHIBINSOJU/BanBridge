/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zaxxer.hikari.HikariConfig
 *  com.zaxxer.hikari.HikariDataSource
 */
package com.shotdevs.banbridge.database;

import com.shotdevs.banbridge.BanBridge;
import com.shotdevs.banbridge.configuration.ConfigManager;
import com.shotdevs.banbridge.database.FileUtil;
import com.shotdevs.banbridge.database.migrations.MigrationRunner;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import javax.sql.DataSource;

public class DatabaseManager {
    private final BanBridge plugin;
    private final ConfigManager config;
    private DataSource dataSource;

    public DatabaseManager(BanBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void initialize() {
        String type = this.config.getStorageType();
        try {
            if ("mysql".equalsIgnoreCase(type) && this.config.isMysqlEnabled()) {
                this.initMySQL();
            } else {
                this.initSQLite();
            }
            this.runMigrations();
            this.plugin.getLogger().info("Database initialized (" + type + ")");
        }
        catch (Exception ex) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", ex);
        }
    }

    private void initSQLite() {
        FileUtil.ensureDirectoryExists(this.plugin.getDataFolder());
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:sqlite:" + new File(this.plugin.getDataFolder(), "banbridge.db").getAbsolutePath());
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(1);
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.addDataSourceProperty("cacheSize", (Object)2000);
        this.dataSource = new HikariDataSource(hikari);
    }

    private void initMySQL() {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", this.config.getMysqlHost(), this.config.getMysqlPort(), this.config.getMysqlDatabase()));
        hikari.setUsername(this.config.getMysqlUsername());
        hikari.setPassword(this.config.getMysqlPassword());
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTestQuery("SELECT 1");
        this.dataSource = new HikariDataSource(hikari);
    }

    private void runMigrations() throws SQLException {
        try (Connection conn = this.getConnection();){
            MigrationRunner.run(conn);
        }
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return this.dataSource.getConnection();
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void shutdown() {
        DataSource dataSource = this.dataSource;
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikari = (HikariDataSource)dataSource;
            hikari.close();
        }
    }
}

