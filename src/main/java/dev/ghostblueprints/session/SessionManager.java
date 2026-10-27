package dev.ghostblueprints.session;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Active blueprint sessions keyed by player. Sessions live in memory only. */
public final class SessionManager {
    private final Map<UUID, BlueprintSession> sessions = new ConcurrentHashMap<>();

    public Optional<BlueprintSession> get(UUID player) {
        return Optional.ofNullable(sessions.get(player));
    }

    public void start(UUID player, BlueprintSession session) {
        sessions.put(player, session);
    }

    public void end(UUID player) {
        sessions.remove(player);
    }

    public Collection<Map.Entry<UUID, BlueprintSession>> all() {
        return sessions.entrySet();
    }
}
