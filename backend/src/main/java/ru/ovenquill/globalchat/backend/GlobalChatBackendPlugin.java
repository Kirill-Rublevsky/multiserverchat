package ru.ovenquill.globalchat.backend;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.ovenquill.globalchat.protocol.SignedPayload;
import java.util.UUID;

public final class GlobalChatBackendPlugin extends JavaPlugin implements Listener {
    private static final String CHANNEL="ovenquill:globalchat"; private String trigger; private String secret; private boolean ready;
    @Override public void onEnable(){ saveDefaultConfig(); trigger=getConfig().getString("trigger","!"); secret=getConfig().getString("shared-secret","");
        if(trigger.isEmpty() || secret.length()<16 || secret.startsWith("CHANGE-ME")){ getLogger().severe("Configure trigger and a matching shared-secret (16+ chars); global messages will be blocked with diagnostics until fixed."); ready=false; }
        else ready=true; getServer().getMessenger().registerOutgoingPluginChannel(this,CHANNEL); getServer().getPluginManager().registerEvents(this,this); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false) public void onChat(AsyncChatEvent event){
        String raw=event.signedMessage().message(); if(!raw.startsWith(trigger)) return;
        event.setCancelled(true); Player player=event.getPlayer();
        if(!ready){ player.sendMessage(Component.text("✦ OVENQUILL | Глобальный чат не настроен на этом сервере. Сообщи администрации.")); getLogger().warning("Blocked global message because backend bridge is not configured"); return; }
        try { byte[] packet=SignedPayload.create(player.getUniqueId(),raw,secret,System.currentTimeMillis(),UUID.randomUUID()).encode();
            getServer().getScheduler().runTask(this,()-> {
                if(player.isOnline()) {
                    player.sendPluginMessage(this,CHANNEL,packet);
                    if (getConfig().getBoolean("debug", false)) getLogger().info("Forwarded global chat packet for " + player.getName());
                }
            });
        } catch(Exception e){ getLogger().log(java.util.logging.Level.SEVERE,"Unable to forward global chat",e); player.sendMessage(Component.text("✦ OVENQUILL | Ошибка отправки глобального сообщения.")); }
    }
}
