package ru.ovenquill.globalchat;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFormatterTest {
    @Test void resolvesDocumentedPlaceholdersAndKeepsPlayerTagsLiteral() {
        GlobalChatConfig config = new GlobalChatConfig(true, "!", "[G]",
                "{prefix} {player} » {message}", true, true, 256, 3,
                true, 10, "secret-long-enough", 30, true, "", false);
        var component = new ChatFormatter().chat(config, "Player", "lobby", "<red>Привет</red>");
        assertEquals("[G] Player » <red>Привет</red>", PlainTextComponentSerializer.plainText().serialize(component));
    }
}
