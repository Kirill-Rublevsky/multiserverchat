package ru.ovenquill.globalchat.backend;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
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
        saveDefaultConfig(); getConfig().options().copyDefaults(true); saveConfig();
        if (!reloadSettings()) { getServer().getPluginManager().disablePlugin(this); return; }
        getServer().getMessenger().registerOutgoingPluginChannel(this,CHANNEL);
        getServer().getPluginManager().registerEvents(this,this);
        getLogger().info("OVENQUILL Chat enabled: global bridge=" + settings.bridgeReady() + ", local chat=" + settings.localEnabled() + ", radius=" + settings.localRadius());
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void onChat(AsyncChatEvent event){
        BackendSettings current=settings;
        String raw=event.signedMessage().message();
        event.setCancelled(true); // Paper acknowledges the signed message; this plugin performs delivery as system chat.
        UUID senderId=event.getPlayer().getUniqueId();
        if(raw.startsWith(current.trigger())) {
            getServer().getScheduler().runTask(this,()->forwardGlobal(senderId,current,raw));
        } else if(current.localEnabled()) {
            Component message=event.message();
            getServer().getScheduler().runTask(this,()->broadcastLocal(senderId,current,message));
        }
    }

    private void forwardGlobal(UUID senderId, BackendSettings current, String raw) {
        Player player=getServer().getPlayer(senderId); if(player==null || !player.isOnline()) return;
        if(!current.bridgeReady()){
            player.sendMessage(Component.text("✦ OVENQUILL | Глобальный чат не настроен на этом сервере. Сообщи администрации."));
            getLogger().warning("Blocked global message because backend bridge is not configured"); return;
        }
        try {
            byte[] packet=SignedPayload.create(player.getUniqueId(),raw,current.secret(),System.currentTimeMillis(),UUID.randomUUID()).encode();
            player.sendPluginMessage(this,CHANNEL,packet);
            if(current.debug()) getLogger().info("Forwarded global chat packet for "+player.getName());
        } catch(Exception e){
            getLogger().log(java.util.logging.Level.SEVERE,"Unable to forward global chat",e);
            player.sendMessage(Component.text("✦ OVENQUILL | Ошибка отправки глобального сообщения."));
        }
    }

    private void broadcastLocal(UUID senderId, BackendSettings current, Component message) {
        Player sender=getServer().getPlayer(senderId); if(sender==null || !sender.isOnline()) return;
        Location origin=sender.getLocation(); double radiusSquared=current.localRadius()*current.localRadius();
        Component rendered=localFormatter.format(current,tabNames.coloredUsername(sender),sender.getWorld().getName(),message);
        int recipients=0;
        for(Player recipient:getServer().getOnlinePlayers()){
            if(recipient.getWorld().equals(sender.getWorld()) && recipient.getLocation().distanceSquared(origin)<=radiusSquared){
                recipient.sendMessage(rendered); recipients++;
            }
        }
        if(current.includeConsole()) getServer().getConsoleSender().sendMessage(rendered);
        if(current.debug()) getLogger().info("Local chat from "+sender.getName()+" delivered to "+recipients+" player(s), world="+sender.getWorld().getName()+", radius="+current.localRadius());
    }

    private boolean reloadSettings(){
        try{ reloadConfig(); settings=BackendSettings.load(getConfig());
            if(!settings.bridgeReady()) getLogger().severe("Configure a matching shared-secret (16+ chars); global messages are blocked until fixed."); return true;
        }catch(RuntimeException e){ getLogger().log(java.util.logging.Level.SEVERE,"Invalid backend configuration",e); return false; }
    }

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!sender.hasPermission("ovenquill.globalchat.backend.admin")){ sender.sendMessage("Недостаточно прав."); return true; }
        if(args.length>0 && args[0].equalsIgnoreCase("reload")){
            sender.sendMessage(reloadSettings()?"OVENQUILL Chat: backend-конфигурация перезагружена.":"Ошибка конфигурации. Проверь консоль."); return true;
        }
        BackendSettings current=settings;
        sender.sendMessage("OVENQUILL Chat: local="+current.localEnabled()+", radius="+current.localRadius()+", global-bridge="+current.bridgeReady()+", debug="+current.debug()); return true;
    }
}
