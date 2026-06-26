package org.chatterjay.visible_energy.network.payloads;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class S2CDeviceHighlightData {

    public record DeviceInfo(BlockPos pos, int deviceTypeOrdinal, String deviceName,
                             String networkName, int networkColor, String energyStatus,
                             boolean isCurrentNetwork, int proportionPercent,
                             int networkId) {}

    private final List<DeviceInfo> devices;

    public S2CDeviceHighlightData(List<DeviceInfo> devices) {
        this.devices = devices;
    }

    public List<DeviceInfo> getDevices() {
        return devices;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(devices.size());
        for (DeviceInfo d : devices) {
            buf.writeBlockPos(d.pos());
            buf.writeVarInt(d.deviceTypeOrdinal());
            buf.writeUtf(d.deviceName(), 100);
            buf.writeUtf(d.networkName(), 100);
            buf.writeInt(d.networkColor());
            buf.writeUtf(d.energyStatus(), 50);
            buf.writeBoolean(d.isCurrentNetwork());
            buf.writeVarInt(d.proportionPercent());
            buf.writeVarInt(d.networkId());
        }
    }

    public static S2CDeviceHighlightData decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<DeviceInfo> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            int typeOrdinal = buf.readVarInt();
            String deviceName = buf.readUtf(100);
            String networkName = buf.readUtf(100);
            int color = buf.readInt();
            String energyStatus = buf.readUtf(50);
            boolean isCurrent = buf.readBoolean();
            int proportion = buf.readVarInt();
            int networkId = buf.readVarInt();
            list.add(new DeviceInfo(pos, typeOrdinal, deviceName, networkName, color, energyStatus,
                    isCurrent, proportion, networkId));
        }
        return new S2CDeviceHighlightData(list);
    }
}
