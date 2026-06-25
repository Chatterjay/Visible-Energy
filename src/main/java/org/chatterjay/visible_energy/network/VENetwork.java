package org.chatterjay.visible_energy.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.chatterjay.visible_energy.VEConstants;
import org.chatterjay.visible_energy.client.DeviceHighlightCache;
import org.chatterjay.visible_energy.network.payloads.S2CDeviceHighlightData;

public class VENetwork {
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VEConstants.MODID);
        registrar.playToClient(
                S2CDeviceHighlightData.TYPE,
                S2CDeviceHighlightData.STREAM_CODEC,
                (data, context) -> {
                    context.enqueueWork(() -> {
                        DeviceHighlightCache.INSTANCE.update(data.devices());
                    });
                }
        );
    }
}
