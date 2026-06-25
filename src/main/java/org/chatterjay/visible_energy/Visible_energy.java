package org.chatterjay.visible_energy;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.chatterjay.visible_energy.command.VECommand;
import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.network.VENetwork;
import org.slf4j.Logger;

@Mod(Visible_energy.MODID)
public class Visible_energy {
    public static final String MODID = "visible_energy";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Visible_energy(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, VEConfig.SPEC);

        modEventBus.addListener(FMLCommonSetupEvent.class, this::onCommonSetup);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class,
                (event) -> VENetwork.register(event));

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                (event) -> VECommand.register(event.getDispatcher()));
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Visible Energy initialized");
    }
}
