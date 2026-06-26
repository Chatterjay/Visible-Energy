package org.chatterjay.visible_energy;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.chatterjay.visible_energy.client.VEClientSetup;
import org.chatterjay.visible_energy.command.ScanSessionTracker;
import org.chatterjay.visible_energy.command.VECommand;
import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.config.VEConfigClient;
import org.chatterjay.visible_energy.network.VENetwork;
import org.slf4j.Logger;

@Mod(VEConstants.MODID)
public class Visible_energy {

    private static final Logger LOGGER = LogUtils.getLogger();

    public Visible_energy() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VEConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, VEConfigClient.SPEC);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            registerConfigScreen();
            VEClientSetup.init();
        }

        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.<RegisterCommandsEvent>addListener(
                e -> VECommand.register(e.getDispatcher()));
        MinecraftForge.EVENT_BUS.<TickEvent.ServerTickEvent>addListener(
                e -> {
                    if (e.phase == TickEvent.Phase.END) {
                        ScanSessionTracker.INSTANCE.onServerTick(e.getServer());
                    }
                });
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        VENetwork.register();
        LOGGER.info("Visible Energy initialized");
    }

    private static void registerConfigScreen() {
        try {
            var screenFactory = new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory(
                    (mc, modsScreen) -> modsScreen);
            ModLoadingContext.get().registerExtensionPoint(
                    net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> screenFactory);
        } catch (Exception e) {
            LOGGER.warn("Failed to register config screen", e);
        }
    }
}
