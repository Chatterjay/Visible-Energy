package org.chatterjay.visible_energy.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import org.chatterjay.visible_energy.client.render.DeviceHighlightRenderer;
import org.chatterjay.visible_energy.config.VEConfig;
import org.chatterjay.visible_energy.config.VEConfigClient;

@OnlyIn(Dist.CLIENT)
public class VEClientSetup {
    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DeviceHighlightRenderer::onRenderLevelStage);
        registerConfigScreen();
    }

    private static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (mc, parent) -> createConfigScreen(parent)));
    }

    private static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("visible_energy.configuration.title"));

        builder.setGlobalized(true);
        builder.setGlobalizedExpanded(false);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory display = builder.getOrCreateCategory(
                Component.translatable("visible_energy.configuration.display"));

        display.addEntry(eb.startIntField(
                        Component.translatable("visible_energy.config.display.displayDuration"),
                        VEConfigClient.DISPLAY_DURATION.get())
                .setDefaultValue(30)
                .setMin(5)
                .setMax(600)
                .setTooltip(Component.translatable("visible_energy.config.display.displayDuration.tooltip"))
                .setSaveConsumer(VEConfigClient.DISPLAY_DURATION::set)
                .build());

        ConfigCategory debounce = builder.getOrCreateCategory(
                Component.translatable("visible_energy.configuration.debounce"));

        debounce.addEntry(eb.startDoubleField(
                        Component.translatable("visible_energy.config.debounce.smoothing"),
                        VEConfigClient.DEBOUNCE_SMOOTHING.get())
                .setDefaultValue(0.4)
                .setMin(0.0)
                .setMax(1.0)
                .setTooltip(Component.translatable("visible_energy.config.debounce.smoothing.tooltip"))
                .setSaveConsumer(VEConfigClient.DEBOUNCE_SMOOTHING::set)
                .build());

        debounce.addEntry(eb.startIntField(
                        Component.translatable("visible_energy.config.debounce.threshold"),
                        VEConfigClient.DEBOUNCE_THRESHOLD.get())
                .setDefaultValue(3)
                .setMin(0)
                .setMax(100)
                .setTooltip(Component.translatable("visible_energy.config.debounce.threshold.tooltip"))
                .setSaveConsumer(VEConfigClient.DEBOUNCE_THRESHOLD::set)
                .build());

        debounce.addEntry(eb.startIntField(
                        Component.translatable("visible_energy.config.debounce.maxMissed"),
                        VEConfigClient.DEBOUNCE_MAX_MISSED.get())
                .setDefaultValue(3)
                .setMin(1)
                .setMax(10)
                .setTooltip(Component.translatable("visible_energy.config.debounce.maxMissed.tooltip"))
                .setSaveConsumer(VEConfigClient.DEBOUNCE_MAX_MISSED::set)
                .build());

        ConfigCategory scanning = builder.getOrCreateCategory(
                Component.translatable("visible_energy.configuration.scanning"));

        scanning.addEntry(eb.startIntField(
                        Component.translatable("visible_energy.config.scanning.scanRadius"),
                        VEConfig.SCAN_RADIUS.get())
                .setDefaultValue(64)
                .setMin(5)
                .setMax(256)
                .setTooltip(Component.translatable("visible_energy.config.scanning.scanRadius.tooltip"))
                .setSaveConsumer(VEConfig.SCAN_RADIUS::set)
                .build());

        builder.setSavingRunnable(() -> {
            VEConfig.SPEC.save();
            VEConfigClient.SPEC.save();
        });

        return builder.build();
    }
}
