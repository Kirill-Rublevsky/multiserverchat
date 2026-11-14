package ru.ovenquill.globalchat;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ConfigManager {
    private final Path directory;
    private volatile GlobalChatConfig config;
    private volatile Map<String,String> messages = Map.of();
    ConfigManager(Path directory) { this.directory = directory; }
    void load() throws IOException {
        Files.createDirectories(directory); copyDefault("config.yml"); copyDefault("messages.yml");
        ConfigurationNode root = YamlConfigurationLoader.builder().path(directory.resolve("config.yml")).build().load();
        GlobalChatConfig nextConfig = GlobalChatConfig.from(root);
        ConfigurationNode msgRoot = YamlConfigurationLoader.builder().path(directory.resolve("messages.yml")).build().load();
        Map<String,String> nextMessages = new ConcurrentHashMap<>();
        for (Map.Entry<Object, ? extends ConfigurationNode> e : msgRoot.childrenMap().entrySet()) nextMessages.put(String.valueOf(e.getKey()), e.getValue().getString(""));
        config = nextConfig; messages = Map.copyOf(nextMessages);
    }
    GlobalChatConfig config() { return config; }
    String message(String key) { return messages.getOrDefault(key, defaultMessage(key)); }
    void persistEnabled(boolean enabled) throws IOException {
        Path path = directory.resolve("config.yml"); YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(path).build();
        ConfigurationNode root = loader.load(); root.node("global-chat", "enabled").set(enabled); loader.save(root); load();
    }
    private void copyDefault(String name) throws IOException {
        Path target = directory.resolve(name); if (Files.exists(target)) return;
        try (var in = getClass().getClassLoader().getResourceAsStream(name)) {
            if (in == null) throw new IOException("Missing resource " + name); Files.copy(in, target);
        }
    }
    private String defaultMessage(String key) {
        return switch (key) {
            case "private-usage" -> "<#A8E6CF>✦ OVENQUILL <gray>| <yellow>Использование: /msg <игрок> <сообщение>";
            case "private-player-only" -> "<red>Эта команда доступна только игрокам.";
            case "private-not-found" -> "<#A8E6CF>✦ OVENQUILL <gray>| <red>Игрок {player} не найден в сети.";
            case "private-self" -> "<#A8E6CF>✦ OVENQUILL <gray>| <red>Нельзя написать самому себе.";
            case "private-invalid" -> "<#A8E6CF>✦ OVENQUILL <gray>| <red>Сообщение содержит недопустимые символы.";
            case "private-incoming" -> "<#A8E6CF>[ЛС] <sender> <#315C4B>→ <#F5F5F5>Вы: <message>";
            case "private-outgoing" -> "<#A8E6CF>[ЛС] <#F5F5F5>Вы <#315C4B>→ <recipient><#F5F5F5>: <message>";
            default -> "<red>Missing message: " + key;
        };
    }
}
