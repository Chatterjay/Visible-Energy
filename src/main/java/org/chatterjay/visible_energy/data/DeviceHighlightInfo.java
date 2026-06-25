package org.chatterjay.visible_energy.data;

import net.minecraft.core.BlockPos;
import sonar.fluxnetworks.api.device.FluxDeviceType;

public class DeviceHighlightInfo {
    private final BlockPos pos;
    private final FluxDeviceType deviceType;
    private final String deviceName;
    private final String networkName;
    private final int networkColor;
    private final String energyStatus;
    private final boolean isCurrentNetwork;
    private final int proportionPercent;
    private final int networkId;
    private final long timestamp;

    public DeviceHighlightInfo(BlockPos pos, int deviceTypeOrdinal, String deviceName,
                                String networkName, int networkColor,
                                String energyStatus, boolean isCurrentNetwork,
                                int proportionPercent, int networkId, long timestamp) {
        this.pos = pos;
        this.deviceType = FluxDeviceType.values()[deviceTypeOrdinal];
        this.deviceName = deviceName;
        this.networkName = networkName;
        this.networkColor = networkColor;
        this.energyStatus = energyStatus;
        this.isCurrentNetwork = isCurrentNetwork;
        this.proportionPercent = proportionPercent;
        this.networkId = networkId;
        this.timestamp = timestamp;
    }

    public BlockPos getPos() { return pos; }
    public FluxDeviceType getDeviceType() { return deviceType; }
    public String getDeviceName() { return deviceName; }
    public String getNetworkName() { return networkName; }
    public int getNetworkColor() { return networkColor; }
    public String getEnergyStatus() { return energyStatus; }
    public boolean isCurrentNetwork() { return isCurrentNetwork; }
    public int getProportionPercent() { return proportionPercent; }
    public int getNetworkId() { return networkId; }

    public boolean isExpired(int durationSeconds) {
        return System.currentTimeMillis() - timestamp > durationSeconds * 1000L;
    }
}
