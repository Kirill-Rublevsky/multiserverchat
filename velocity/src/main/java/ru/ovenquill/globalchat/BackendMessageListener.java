package ru.ovenquill.globalchat;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;
import ru.ovenquill.globalchat.protocol.SignedPayload;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class BackendMessageListener {
    static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("ovenquill:globalchat");
    private final GlobalChatPlugin plugin; private final Logger logger; private final ConcurrentHashMap<UUID,Long> nonces = new ConcurrentHashMap<>();
    BackendMessageListener(GlobalChatPlugin plugin, Logger logger) { this.plugin=plugin; this.logger=logger; }
    @Subscribe public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection source)) { logger.warn("Rejected global chat packet from non-server source"); return; }
        try {
            SignedPayload payload = SignedPayload.decode(event.getData()); GlobalChatConfig c = plugin.configs().config(); long now = System.currentTimeMillis();
            if (c.debug()) logger.info("Received global chat packet from {} for {}", source.getServerInfo().getName(), payload.playerId());
            if (!payload.verify(c.sharedSecret(), now, c.packetMaxAgeSeconds()*1000L)) { logger.warn("Rejected invalid global chat signature from {}", source.getServerInfo().getName()); return; }
            if (nonces.putIfAbsent(payload.nonce(), now) != null) { logger.warn("Rejected replayed global chat packet"); return; }
            nonces.entrySet().removeIf(e -> now-e.getValue() > c.packetMaxAgeSeconds()*2000L);
            Player player = plugin.proxy().getPlayer(payload.playerId()).orElse(null);
            if (player == null || player.getCurrentServer().map(s -> !s.getServerInfo().equals(source.getServerInfo())).orElse(true)) { logger.warn("Rejected player/server mismatch for {}", payload.playerId()); return; }
            plugin.service().accept(player, payload.message());
        } catch (Exception e) { logger.warn("Rejected malformed global chat packet", e); }
    }
}
