package ru.ovenquill.globalchat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

final class ChatFormatter {
    private final MiniMessage mini = MiniMessage.miniMessage();
    Component chat(GlobalChatConfig c, Component player, String server, String message) {
        String template = c.format()
                .replace("{prefix}", "<prefix>")
                .replace("{player}", "<player>")
                .replace("{server}", "<server>")
                .replace("{message}", "<message>");
        return mini.deserialize(template, Placeholder.unparsed("prefix", c.displayPrefix()), Placeholder.component("player", player),
                Placeholder.unparsed("server", server), Placeholder.unparsed("message", message));
    }
    Component system(String template, String... replacements) {
        String value = template; for (int i=0; i<replacements.length; i+=2) value = value.replace("{" + replacements[i] + "}", replacements[i+1]);
        return mini.deserialize(value);
    }
    Component privateIncoming(String template, Component sender, String message) {
        return mini.deserialize(toTags(template), Placeholder.component("sender", sender), Placeholder.unparsed("message", message));
    }
    Component privateOutgoing(String template, Component recipient, String message) {
        return mini.deserialize(toTags(template), Placeholder.component("recipient", recipient), Placeholder.unparsed("message", message));
    }
    private String toTags(String template) {
        return template.replace("{sender}", "<sender>").replace("{recipient}", "<recipient>").replace("{message}", "<message>");
    }
}
