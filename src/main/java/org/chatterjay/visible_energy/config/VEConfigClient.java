package org.chatterjay.visible_energy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class VEConfigClient {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue DISPLAY_DURATION;
    public static final ForgeConfigSpec.DoubleValue DEBOUNCE_SMOOTHING;
    public static final ForgeConfigSpec.IntValue DEBOUNCE_THRESHOLD;
    public static final ForgeConfigSpec.IntValue DEBOUNCE_MAX_MISSED;

    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("display");
        DISPLAY_DURATION = BUILDER
                .comment("Duration in seconds to display highlights")
                .defineInRange("displayDuration", 30, 5, 600);
        BUILDER.pop();

        BUILDER.push("debounce");
        DEBOUNCE_SMOOTHING = BUILDER
                .comment("EMA smoothing factor for energy proportion (0.0 = no smoothing, 1.0 = instant)")
                .defineInRange("smoothing", 0.4, 0.0, 1.0);
        DEBOUNCE_THRESHOLD = BUILDER
                .comment("Minimum proportion change (%) to update energy status text")
                .defineInRange("thresholdPercent", 3, 0, 100);
        DEBOUNCE_MAX_MISSED = BUILDER
                .comment("Consecutive missed scans before removing a device")
                .defineInRange("maxMissedScans", 3, 1, 10);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
