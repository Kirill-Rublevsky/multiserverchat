package ru.ovenquill.globalchat;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.audience.Audience;
import java.util.List;

final class GlobalChatCommand implements SimpleCommand {
    private final GlobalChatPlugin plugin; private final ChatFormatter formatter = new ChatFormatter();
    GlobalChatCommand(GlobalChatPlugin plugin) { this.plugin=plugin; }
    @Override public void execute(Invocation invocation) {
        String sub = invocation.arguments().length == 0 ? "help" : invocation.arguments()[0].toLowerCase(); Audience source=invocation.source();
        switch(sub) {
            case "reload" -> { if (!allowed(invocation,"ovenquill.globalchat.reload")) return; try { plugin.configs().load(); send(source,"reload-success"); } catch(Exception e) { plugin.logger().error("Reload failed",e); send(source,"reload-error"); } }
            case "status" -> { if (!allowed(invocation,"ovenquill.globalchat.admin")) return; GlobalChatConfig c=plugin.configs().config(); sendRaw(source, plugin.configs().message("status"), "enabled", c.enabled()?"включён":"выключен", "trigger", c.trigger(), "players", String.valueOf(plugin.proxy().getPlayerCount()), "backends", String.valueOf(plugin.proxy().getAllServers().size())); }
            case "toggle" -> { if (!allowed(invocation,"ovenquill.globalchat.admin")) return; try { boolean next=!plugin.configs().config().enabled(); plugin.configs().persistEnabled(next); sendRaw(source,plugin.configs().message("toggle"),"enabled",next?"включён":"выключен"); plugin.logger().info("{} toggled global chat to {}", invocation.source(), next); } catch(Exception e){ plugin.logger().error("Toggle persistence failed",e); send(source,"reload-error"); } }
            default -> send(source,"help");
        }
    }
    private boolean allowed(Invocation i,String p){ if(i.source().hasPermission(p)) return true; send(i.source(),"no-permission"); return false; }
    private void send(Audience a,String key){ sendRaw(a,plugin.configs().message("prefix")+plugin.configs().message(key)); }
    private void sendRaw(Audience a,String value,String... replacements){ a.sendMessage(formatter.system(value,replacements)); }
    @Override public List<String> suggest(Invocation i){ return List.of("help","reload","status","toggle"); }
}
