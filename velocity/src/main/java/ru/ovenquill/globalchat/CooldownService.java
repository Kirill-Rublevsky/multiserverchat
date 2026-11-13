package ru.ovenquill.globalchat;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class CooldownService {
    private record State(long acceptedAt, String normalizedMessage) {}
    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();
    record Decision(boolean allowed, String reason, long seconds) { static Decision ok(){ return new Decision(true,"",0); } }
    Decision check(UUID id, String message, long now, GlobalChatConfig c, boolean bypass) {
        String normalized = message.strip().toLowerCase(java.util.Locale.ROOT);
        final Decision[] result = {Decision.ok()};
        states.compute(id, (key, previous) -> {
            if (previous != null && c.duplicateEnabled() && normalized.equals(previous.normalizedMessage()) && now - previous.acceptedAt() < c.duplicateSeconds()*1000L) {
                result[0] = new Decision(false,"duplicate",0); return previous;
            }
            if (!bypass && previous != null && now - previous.acceptedAt() < c.cooldownSeconds()*1000L) {
                long remaining = Math.max(1, (c.cooldownSeconds()*1000L - (now-previous.acceptedAt()) + 999)/1000); result[0] = new Decision(false,"cooldown",remaining); return previous;
            }
            return new State(now, normalized);
        }); return result[0];
    }
    void remove(UUID id) { states.remove(id); }
}
