package org.chatterjay.visible_energy.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.chatterjay.visible_energy.config.VEConfigClient;
import org.chatterjay.visible_energy.data.DeviceHighlightInfo;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData.DeviceInfo;

import net.minecraft.core.BlockPos;

public enum DeviceHighlightCache {
    INSTANCE;

    private final Map<DeviceKey, DeviceEntry> deviceMap = new ConcurrentHashMap<>();

    public void update(List<DeviceInfo> rawDevices) {
        if (rawDevices.isEmpty()) {
            deviceMap.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Set<DeviceKey> seen = new HashSet<>();

        for (var d : rawDevices) {
            DeviceKey key = new DeviceKey(d.pos(), d.networkId());
            seen.add(key);
            DeviceEntry existing = deviceMap.get(key);

            if (existing != null) {
                existing.refresh(d, now);
            } else {
                deviceMap.put(key, new DeviceEntry(d, now));
            }
        }

        int maxMissed = VEConfigClient.DEBOUNCE_MAX_MISSED.get();
        deviceMap.entrySet().removeIf(entry -> {
            if (!seen.contains(entry.getKey())) {
                entry.getValue().missedCount++;
                return entry.getValue().missedCount > maxMissed;
            }
            return false;
        });
    }

    public List<DeviceHighlightInfo> getActiveHighlights() {
        int duration = VEConfigClient.DISPLAY_DURATION.get();
        long expireCutoff = System.currentTimeMillis() - duration * 1000L;

        List<DeviceHighlightInfo> result = new ArrayList<>(deviceMap.size());
        deviceMap.entrySet().removeIf(entry -> {
            if (entry.getValue().timestamp < expireCutoff) {
                return true;
            }
            return false;
        });

        for (DeviceEntry entry : deviceMap.values()) {
            if (entry.timestamp >= expireCutoff) {
                result.add(entry.toDisplayInfo());
            }
        }

        return result;
    }

    public void clear() {
        deviceMap.clear();
    }

    private record DeviceKey(BlockPos pos, int networkId) {}

    private static class DeviceEntry {
        final BlockPos pos;
        final int deviceTypeOrdinal;
        final String deviceName;
        final String networkName;
        final int networkColor;
        final boolean isCurrentNetwork;
        final int networkId;

        int smoothedProportion;
        String energyStatus;
        long timestamp;
        int missedCount;

        DeviceEntry(DeviceInfo info, long now) {
            this.pos = info.pos();
            this.deviceTypeOrdinal = info.deviceTypeOrdinal();
            this.deviceName = info.deviceName();
            this.networkName = info.networkName();
            this.networkColor = info.networkColor();
            this.isCurrentNetwork = info.isCurrentNetwork();
            this.networkId = info.networkId();
            this.smoothedProportion = info.proportionPercent();
            this.energyStatus = info.energyStatus();
            this.timestamp = now;
        }

        void refresh(DeviceInfo info, long now) {
            this.timestamp = now;
            this.missedCount = 0;

            int rawProportion = info.proportionPercent();

            if (!info.energyStatus().equals(this.energyStatus)) {
                this.energyStatus = info.energyStatus();
            }

            double smoothing = VEConfigClient.DEBOUNCE_SMOOTHING.get();
            this.smoothedProportion = (int) Math.round(
                    smoothedProportion * (1.0 - smoothing) + rawProportion * smoothing);

            if (Math.abs(rawProportion - this.smoothedProportion) > VEConfigClient.DEBOUNCE_THRESHOLD.get()) {
                this.energyStatus = info.energyStatus();
            }
        }

        DeviceHighlightInfo toDisplayInfo() {
            return new DeviceHighlightInfo(
                    pos, deviceTypeOrdinal, deviceName, networkName, networkColor,
                    energyStatus, isCurrentNetwork, smoothedProportion, networkId, timestamp);
        }
    }
}
