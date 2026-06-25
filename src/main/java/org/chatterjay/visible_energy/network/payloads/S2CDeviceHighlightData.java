package org.chatterjay.visible_energy.network.payloads;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import org.chatterjay.visible_energy.VEConstants;

public record S2CDeviceHighlightData(List<DeviceInfo> devices) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2CDeviceHighlightData> TYPE =
            new CustomPacketPayload.Type<>(VEConstants.DEVICE_HIGHLIGHT_PACKET_ID);

    public static final StreamCodec<FriendlyByteBuf, S2CDeviceHighlightData> STREAM_CODEC =
            StreamCodec.ofMember(S2CDeviceHighlightData::write, S2CDeviceHighlightData::new);

    public record DeviceInfo(BlockPos pos, int deviceTypeOrdinal, String deviceName,
                             String networkName, int networkColor, String energyStatus,
                             boolean isCurrentNetwork, int proportionPercent,
                             int networkId) {}

    public S2CDeviceHighlightData(FriendlyByteBuf buf) {
        this(readDevices(buf));
    }

    public void write(FriendlyByteBuf buf) {
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

    private static List<DeviceInfo> readDevices(FriendlyByteBuf buf) {
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
        return list;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
