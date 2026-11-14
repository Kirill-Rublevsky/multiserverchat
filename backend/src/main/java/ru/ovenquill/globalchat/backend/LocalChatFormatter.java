package ru.ovenquill.globalchat.backend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

final class LocalChatFormatter {
    private final MiniMessage mini = MiniMessage.miniMessage();
    Component format(BackendSettings settings, Component player, String world, Component message) {
        String template = settings.localFormat()
                .replace("{prefix}", "<prefix>")
                .replace("{player}", "<player>")
                .replace("{world}", "<world>")
                .replace("{message}", "<message>");
        return mini.deserialize(template,
                Placeholder.unparsed("prefix", settings.localPrefix()),
                Placeholder.component("player", player),
                Placeholder.unparsed("world", world),
                Placeholder.component("message", message));
    }
}
