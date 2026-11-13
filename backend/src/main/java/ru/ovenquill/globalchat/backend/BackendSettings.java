package ru.ovenquill.globalchat.backend;

import org.bukkit.configuration.file.FileConfiguration;

record BackendSettings(String trigger, String secret, boolean debug, boolean localEnabled,
                       double localRadius, String localPrefix, String localFormat, boolean includeConsole) {
    static BackendSettings load(FileConfiguration config) {
        String trigger = config.getString("trigger", "!");
        String secret = config.getString("shared-secret", "");
        double radius = config.getDouble("local-chat.radius", 100.0);
        if (trigger == null || trigger.isEmpty()) throw new IllegalArgumentException("trigger cannot be empty");
        if (radius < 0 || !Double.isFinite(radius)) throw new IllegalArgumentException("local-chat.radius must be a finite non-negative number");
        return new BackendSettings(trigger, secret == null ? "" : secret, config.getBoolean("debug", false),
                config.getBoolean("local-chat.enabled", true), radius,
                config.getString("local-chat.display-prefix", "[L]"),
                config.getString("local-chat.format", "<#A8E6CF>{prefix} <#C8A2FF>{player} <#315C4B>» <#F5F5F5>{message}"),
                config.getBoolean("local-chat.include-console", true));
    }
    boolean bridgeReady() { return secret.length() >= 16 && !secret.startsWith("CHANGE-ME"); }
}
