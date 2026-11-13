package ru.ovenquill.globalchat;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import java.util.Objects;

final class GlobalChatService {
    private final ProxyServer proxy; private final ConfigManager configs; private final ChatFormatter formatter; private final CooldownService cooldown; private final Logger logger;
    GlobalChatService(ProxyServer proxy, ConfigManager configs, ChatFormatter formatter, CooldownService cooldown, Logger logger) {
        this.proxy=proxy; this.configs=configs; this.formatter=formatter; this.cooldown=cooldown; this.logger=logger;
    }
    void accept(Player player, String raw) {
        GlobalChatConfig c = configs.config();
        if (!c.enabled()) { reply(player,"chat-disabled"); return; }
        if (!raw.startsWith(c.trigger())) return;
        String message = raw.substring(c.trigger().length()).strip();
        if (message.isEmpty()) { reply(player,"empty-message"); return; }
        int length = message.codePointCount(0, message.length());
        if (length > c.maxLength()) { reply(player,"too-long","max",String.valueOf(c.maxLength())); return; }
        if (message.codePoints().anyMatch(cp -> Character.isISOControl(cp) && cp != '\t')) { reply(player,"backend-error"); return; }
        CooldownService.Decision decision = cooldown.check(player.getUniqueId(), message, System.currentTimeMillis(), c, player.hasPermission("ovenquill.globalchat.bypasscooldown"));
        if (!decision.allowed()) { reply(player, decision.reason(), "seconds", String.valueOf(decision.seconds())); return; }
        String server = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown");
        var component = formatter.chat(c, player.getUsername(), server, message);
        for (Player recipient : proxy.getAllPlayers()) {
            if (!c.includeSender() && recipient.getUniqueId().equals(player.getUniqueId())) continue;
            try { recipient.sendMessage(component); } catch (RuntimeException e) { logger.warn("Unable to deliver global chat to {}", recipient.getUsername(), e); }
        }
        if (c.consoleLog() && c.loggingEnabled()) logger.info(c.logFormat().replace("{player}", player.getUsername()).replace("{message}", message).replace("{server}", server));
    }
    void reply(Player player, String key, String... replacements) { player.sendMessage(formatter.system(configs.message("prefix") + configs.message(key), replacements)); }
}
