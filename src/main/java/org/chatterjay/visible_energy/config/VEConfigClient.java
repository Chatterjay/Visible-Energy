package org.chatterjay.visible_energy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VEConfigClient {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DISPLAY_DURATION;
    public static final ModConfigSpec.DoubleValue DEBOUNCE_SMOOTHING;
    public static final ModConfigSpec.IntValue DEBOUNCE_THRESHOLD;
    public static final ModConfigSpec.IntValue DEBOUNCE_MAX_MISSED;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("display");
        DISPLAY_DURATION = BUILDER
                .comment("Duration in seconds to display highlights")
                .translation("visible_energy.config.display.displayDuration")
                .defineInRange("displayDuration", 30, 5, 600);
        BUILDER.pop();

        BUILDER.push("debounce");
        DEBOUNCE_SMOOTHING = BUILDER
                .comment("EMA smoothing factor for energy proportion (0.0 = no smoothing, 1.0 = instant)")
                .translation("visible_energy.config.debounce.smoothing")
                .defineInRange("smoothing", 0.4, 0.0, 1.0);
        DEBOUNCE_THRESHOLD = BUILDER
                .comment("Minimum proportion change (%) to update energy status text")
                .translation("visible_energy.config.debounce.threshold")
                .defineInRange("thresholdPercent", 3, 0, 100);
        DEBOUNCE_MAX_MISSED = BUILDER
                .comment("Consecutive missed scans before removing a device")
                .translation("visible_energy.config.debounce.maxMissed")
                .defineInRange("maxMissedScans", 3, 1, 10);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
