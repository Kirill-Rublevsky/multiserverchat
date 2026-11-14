package ru.ovenquill.globalchat;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class PrivateMessageCommand implements SimpleCommand {
    private final GlobalChatPlugin plugin;
    private final ChatFormatter formatter = new ChatFormatter();
    private final TabNameResolver tabNames = new TabNameResolver();
    PrivateMessageCommand(GlobalChatPlugin plugin) { this.plugin = plugin; }

    @Override public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player sender)) {
            invocation.source().sendMessage(formatter.system(plugin.configs().message("private-player-only"))); return;
        }
        String[] args = invocation.arguments();
        if (args.length < 2) { sender.sendMessage(formatter.system(plugin.configs().message("private-usage"))); return; }
        Player recipient = plugin.proxy().getPlayer(args[0]).orElse(null);
        if (recipient == null) { sender.sendMessage(formatter.system(plugin.configs().message("private-not-found"), "player", args[0])); return; }
        if (recipient.getUniqueId().equals(sender.getUniqueId())) { sender.sendMessage(formatter.system(plugin.configs().message("private-self"))); return; }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).strip();
        if (message.isEmpty()) { sender.sendMessage(formatter.system(plugin.configs().message("private-usage"))); return; }
        int max = plugin.configs().config().maxLength();
        if (message.codePointCount(0, message.length()) > max) { sender.sendMessage(formatter.system(plugin.configs().message("too-long"), "max", String.valueOf(max))); return; }
        if (message.codePoints().anyMatch(cp -> Character.isISOControl(cp) && cp != '\t')) { sender.sendMessage(formatter.system(plugin.configs().message("private-invalid"))); return; }
        sender.sendMessage(formatter.privateOutgoing(plugin.configs().message("private-outgoing"), tabNames.coloredUsername(recipient), message));
        recipient.sendMessage(formatter.privateIncoming(plugin.configs().message("private-incoming"), tabNames.coloredUsername(sender), message));
    }

    @Override public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length > 1) return List.of();
        String prefix = invocation.arguments().length == 0 ? "" : invocation.arguments()[0].toLowerCase(Locale.ROOT);
        return plugin.proxy().getAllPlayers().stream().map(Player::getUsername)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
