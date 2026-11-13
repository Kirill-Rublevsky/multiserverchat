package ru.ovenquill.globalchat;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import java.nio.file.Path;

@Plugin(id="ovenquill-globalchat", name="OVENQUILL GlobalChat", version="1.0.1", authors={"OVENQUILL"})
public final class GlobalChatPlugin {
    private final ProxyServer proxy; private final Logger logger; private final ConfigManager configs; private final CooldownService cooldown = new CooldownService(); private GlobalChatService service;
    @Inject public GlobalChatPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory){ this.proxy=proxy; this.logger=logger; this.configs=new ConfigManager(dataDirectory); }
    @Subscribe public void onInitialize(ProxyInitializeEvent ignored) {
        try { configs.load(); } catch(Exception e){ logger.error("Cannot load OVENQUILL GlobalChat configuration; plugin will not start",e); return; }
        proxy.getChannelRegistrar().register(BackendMessageListener.CHANNEL); service=new GlobalChatService(proxy,configs,new ChatFormatter(),cooldown,logger);
        proxy.getEventManager().register(this,new BackendMessageListener(this,logger));
        CommandMeta meta=proxy.getCommandManager().metaBuilder("globalchat").aliases("gc").plugin(this).build(); proxy.getCommandManager().register(meta,new GlobalChatCommand(this));
        logger.info("OVENQUILL GlobalChat enabled in signed-safe Velocity + Purpur mode");
    }
    @Subscribe public void onDisconnect(DisconnectEvent event){ cooldown.remove(event.getPlayer().getUniqueId()); }
    ProxyServer proxy(){ return proxy; } Logger logger(){ return logger; } ConfigManager configs(){ return configs; } GlobalChatService service(){ return service; }
}
