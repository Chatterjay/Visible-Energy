package org.chatterjay.visible_energy;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.chatterjay.visible_energy.command.ScanSessionTracker;
import org.chatterjay.visible_energy.command.VECommand;
import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.config.VEConfigClient;
import org.chatterjay.visible_energy.network.VENetwork;
import org.slf4j.Logger;

@Mod(VEConstants.MODID)
public class Visible_energy {
    private static final Logger LOGGER = LogUtils.getLogger();

    public Visible_energy(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, VEConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, VEConfigClient.SPEC);

        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            registerConfigScreen(modContainer);
        }

        modEventBus.addListener(FMLCommonSetupEvent.class, this::onCommonSetup);
        modEventBus.addListener(RegisterPayloadHandlersEvent.class,
                (event) -> VENetwork.register(event));

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                (event) -> VECommand.register(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class,
                (event) -> ScanSessionTracker.INSTANCE.onServerTick(event.getServer()));
    }

    private static void registerConfigScreen(ModContainer container) {
        try {
            Class<?> factoryClass = Class.forName("net.neoforged.neoforge.client.gui.IConfigScreenFactory");
            Class<?> configScreenClass = Class.forName("net.neoforged.neoforge.client.gui.ConfigurationScreen");
            Class<?> screenClass = Class.forName("net.minecraft.client.gui.screens.Screen");
            var ctor = configScreenClass.getConstructor(ModContainer.class, screenClass);

            Object factory = java.lang.reflect.Proxy.newProxyInstance(
                    factoryClass.getClassLoader(),
                    new Class<?>[]{factoryClass},
                    (_proxy, method, args) -> {
                        String name = method.getName();
                        if ("createScreen".equals(name) && args != null && args.length == 2) {
                            return ctor.newInstance(container, args[1]);
                        }
                        if ("toString".equals(name)) return "ConfigScreenFactory";
                        if ("hashCode".equals(name)) return System.identityHashCode(_proxy);
                        if ("equals".equals(name) && args != null && args.length == 1) return _proxy == args[0];
                        return null;
                    }
            );

            var regMethod = ModContainer.class.getMethod("registerExtensionPoint", Class.class, java.util.function.Supplier.class);
            regMethod.invoke(container, factoryClass, (java.util.function.Supplier<?>) () -> factory);
        } catch (Exception e) {
            LOGGER.warn("Failed to register config screen", e);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Visible Energy initialized");
    }
}
