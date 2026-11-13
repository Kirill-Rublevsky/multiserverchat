package ru.ovenquill.globalchat.backend;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

final class TabNameResolver {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    Component coloredUsername(Player player) {
        Component tabName = player.playerListName();
        TextColor color = findUsernameColor(tabName, player.getName(), null);
        return color == null ? Component.text(player.getName()) : Component.text(player.getName(), color);
    }
    private TextColor findUsernameColor(Component component, String username, TextColor inherited) {
        TextColor effective = component.color() == null ? inherited : component.color();
        for (Component child : component.children()) {
            if (PLAIN.serialize(child).contains(username)) {
                TextColor found = findUsernameColor(child, username, effective);
                if (found != null) return found;
            }
        }
        return PLAIN.serialize(component).contains(username) ? effective : null;
    }
}
