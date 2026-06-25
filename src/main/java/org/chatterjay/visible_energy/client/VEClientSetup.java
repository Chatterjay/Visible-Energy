package org.chatterjay.visible_energy.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.chatterjay.visible_energy.client.render.DeviceHighlightRenderer;

import static org.chatterjay.visible_energy.VEConstants.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class VEClientSetup {

    @SubscribeEvent
    static void setup(final FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class,
                DeviceHighlightRenderer::onRenderLevelStage);
    }
}
