package org.chatterjay.visible_energy.command;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public enum ScanSessionTracker {
    INSTANCE;

    private static final int SCAN_INTERVAL_TICKS = 20;

    private final Map<UUID, ScanSession> sessions = new ConcurrentHashMap<>();

    public void startSession(ServerPlayer player, int radius, int durationSeconds) {
        sessions.put(player.getUUID(), new ScanSession(radius,
                System.currentTimeMillis() + durationSeconds * 1000L));
    }

    public void stopSession(UUID uuid) {
        sessions.remove(uuid);
    }

    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public void onServerTick(MinecraftServer server) {
        if (sessions.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, ScanSession>> it = sessions.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, ScanSession> entry = it.next();
            ScanSession session = entry.getValue();

            if (now > session.expiryTime) {
                it.remove();
                continue;
            }

            session.tickCount++;
            if (session.tickCount % SCAN_INTERVAL_TICKS != 0) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
                continue;
            }

            VECommand.scanAndSend(player, session.radius);
        }
    }

    private static class ScanSession {
        final int radius;
        final long expiryTime;
        int tickCount;

        ScanSession(int radius, long expiryTime) {
            this.radius = radius;
            this.expiryTime = expiryTime;
        }
    }
}
