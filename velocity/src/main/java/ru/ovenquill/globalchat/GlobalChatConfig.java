package ru.ovenquill.globalchat;

import org.spongepowered.configurate.ConfigurationNode;

record GlobalChatConfig(boolean enabled, String trigger, String displayPrefix, String format,
                        boolean includeSender, boolean consoleLog, int maxLength, int cooldownSeconds,
                        boolean duplicateEnabled, int duplicateSeconds, String sharedSecret,
                        int packetMaxAgeSeconds, boolean loggingEnabled, String logFormat, boolean debug) {
    static GlobalChatConfig from(ConfigurationNode n) {
        String trigger = n.node("global-chat", "trigger").getString("!");
        if (trigger.isEmpty()) throw new IllegalArgumentException("global-chat.trigger cannot be empty");
        String secret = n.node("backend", "shared-secret").getString("");
        if (secret.length() < 16 || secret.startsWith("CHANGE-ME"))
            throw new IllegalArgumentException("backend.shared-secret must be changed and contain at least 16 characters");
        return new GlobalChatConfig(
                n.node("global-chat", "enabled").getBoolean(true), trigger,
                n.node("global-chat", "display-prefix").getString("[G]"),
                n.node("global-chat", "format").getString("<#A8E6CF>{prefix} <#C8A2FF>{player} <#315C4B>» <#F5F5F5>{message}"),
                n.node("global-chat", "include-sender").getBoolean(true), n.node("global-chat", "console-log").getBoolean(true),
                positive(n, "limits", "max-message-length", 256), positive(n, "limits", "cooldown-seconds", 3),
                n.node("limits", "duplicate-protection", "enabled").getBoolean(true), positive(n, "limits", "duplicate-protection", "interval-seconds", 10),
                secret, positive(n, "backend", "packet-max-age-seconds", 30), n.node("logging", "enabled").getBoolean(true),
                n.node("logging", "log-format").getString("[GLOBAL] {player}: {message}"), n.node("plugin", "debug").getBoolean(false));
    }
    private static int positive(ConfigurationNode n, String a, String b, int fallback) { return positive(n, a, b, null, fallback); }
    private static int positive(ConfigurationNode n, String a, String b, String c, int fallback) {
        ConfigurationNode node = c == null ? n.node(a,b) : n.node(a,b,c); int value = node.getInt(fallback);
        if (value < 0) throw new IllegalArgumentException(String.join(".", a,b,c == null ? "" : c) + " cannot be negative"); return value;
    }
}
