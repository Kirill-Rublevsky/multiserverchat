package ru.ovenquill.globalchat.backend;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.ovenquill.globalchat.protocol.SignedPayload;
import java.util.UUID;

public final class GlobalChatBackendPlugin extends JavaPlugin implements Listener {
    private static final String CHANNEL="ovenquill:globalchat";
    private final LocalChatFormatter localFormatter = new LocalChatFormatter();
    private final TabNameResolver tabNames = new TabNameResolver();
    private volatile BackendSettings settings;

    @Override public void onEnable(){
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        if (!reloadSettings()) { getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getMessenger().registerOutgoingPluginChannel(this,CHANNEL);
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("OVENQUILL Chat enabled: global bridge=" + settings.bridgeReady() + ", local chat=" + settings.localEnabled() + ", radius=" + settings.localRadius());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false) public void onChat(AsyncChatEvent event){
        BackendSettings current = settings;
        String raw=event.signedMessage().message();
        if(raw.startsWith(current.trigger())) { handleGlobal(event, current, raw); return; }
        if(current.localEnabled()) handleLocal(event, current, raw);
    }

    private void handleGlobal(AsyncChatEvent event, BackendSettings current, String raw) {
        event.setCancelled(true); Player player=event.getPlayer();
        if(!current.bridgeReady()){
            player.sendMessage(Component.text("✦ OVENQUILL | Глобальный чат не настроен на этом сервере. Сообщи администрации."));
            getLogger().warning("Blocked global message because backend bridge is not configured"); return;
        }
        try {
            byte[] packet=SignedPayload.create(player.getUniqueId(),raw,current.secret(),System.currentTimeMillis(),UUID.randomUUID()).encode();
            getServer().getScheduler().runTask(this,()-> {
                if(player.isOnline()) {
                    player.sendPluginMessage(this,CHANNEL,packet);
                    if (current.debug()) getLogger().info("Forwarded global chat packet for " + player.getName());
                }
            });
        } catch(Exception e){
            getLogger().log(java.util.logging.Level.SEVERE,"Unable to forward global chat",e);
            player.sendMessage(Component.text("✦ OVENQUILL | Ошибка отправки глобального сообщения."));
        }
    }

    private void handleLocal(AsyncChatEvent event, BackendSettings current, String raw) {
        Player sender = event.getPlayer();
        double radiusSquared = current.localRadius() * current.localRadius();
        event.setCancelled(false);
        event.viewers().removeIf(audience -> {
            if (audience instanceof Player recipient) {
                return !recipient.getWorld().equals(sender.getWorld()) || recipient.getLocation().distanceSquared(sender.getLocation()) > radiusSquared;
            }
            return !current.includeConsole();
        });
        event.viewers().add(sender);
        Component rendered = localFormatter.format(current, tabNames.coloredUsername(sender), sender.getWorld().getName(), raw);
        event.renderer(ChatRenderer.viewerUnaware((source, sourceDisplayName, message) -> rendered));
    }

    private boolean reloadSettings() {
        try {
            reloadConfig(); settings=BackendSettings.load(getConfig());
            if (!settings.bridgeReady()) getLogger().severe("Configure a matching shared-secret (16+ chars); global messages are blocked until fixed.");
            return true;
        } catch (RuntimeException e) { getLogger().log(java.util.logging.Level.SEVERE,"Invalid backend configuration",e); return false; }
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ovenquill.globalchat.backend.admin")) { sender.sendMessage("Недостаточно прав."); return true; }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (reloadSettings()) sender.sendMessage("OVENQUILL Chat: backend-конфигурация перезагружена.");
            else sender.sendMessage("Ошибка конфигурации. Проверь консоль.");
            return true;
        }
        BackendSettings current=settings;
        sender.sendMessage("OVENQUILL Chat: local=" + current.localEnabled() + ", radius=" + current.localRadius() + ", global-bridge=" + current.bridgeReady());
        return true;
    }
}
