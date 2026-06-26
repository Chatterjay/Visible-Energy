package org.chatterjay.visible_energy.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.chatterjay.visible_energy.client.render.DeviceHighlightRenderer;

@OnlyIn(Dist.CLIENT)
public class VEClientSetup {
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DeviceHighlightRenderer::onRenderLevelStage);
    }
}
