package org.chatterjay.visible_energy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.chatterjay.visible_energy.VEConstants;
import org.chatterjay.visible_energy.client.DeviceHighlightCache;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData;

public class VENetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(VEConstants.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, S2CDeviceHighlightData.class,
                S2CDeviceHighlightData::encode,
                S2CDeviceHighlightData::decode,
                (data, ctx) -> {
                    ctx.get().enqueueWork(() ->
                            DeviceHighlightCache.INSTANCE.update(data.getDevices()));
                    ctx.get().setPacketHandled(true);
                });
    }
}
