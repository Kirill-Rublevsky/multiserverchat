package ru.ovenquill.globalchat;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

final class TabNameResolver {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    Component coloredUsername(Player player) {
        Component tabName = player.getTabList().getEntry(player.getUniqueId())
                .flatMap(entry -> entry.getDisplayNameComponent()).orElse(Component.text(player.getUsername()));
        TextColor color = findUsernameColor(tabName, player.getUsername(), null);
        return color == null ? Component.text(player.getUsername()) : Component.text(player.getUsername(), color);
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
