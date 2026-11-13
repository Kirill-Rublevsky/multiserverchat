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
    String message(String key) { return messages.getOrDefault(key, "<red>Missing message: " + key); }
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
}
