package org.chatterjay.visible_energy.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.data.DeviceHighlightInfo;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData.DeviceInfo;

public enum DeviceHighlightCache {
    INSTANCE;

    private final List<DeviceHighlightInfo> devices = new CopyOnWriteArrayList<>();

    public void update(List<DeviceInfo> rawDevices) {
        devices.clear();
        long now = System.currentTimeMillis();
        for (var d : rawDevices) {
            devices.add(new DeviceHighlightInfo(
                    d.pos(), d.deviceTypeOrdinal(), d.deviceName(),
                    d.networkName(), d.networkColor(), d.energyStatus(),
                    d.isCurrentNetwork(), d.proportionPercent(), d.networkId(), now));
        }
    }

    public List<DeviceHighlightInfo> getActiveHighlights() {
        int duration = VEConfig.DISPLAY_DURATION.get();
        devices.removeIf(info -> info.isExpired(duration));
        return List.copyOf(devices);
    }
}
