package org.chatterjay.visible_energy.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class VEConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SCAN_RADIUS;

    public static final ForgeConfigSpec SPEC;

    static {
        SCAN_RADIUS = BUILDER
                .comment("Scan radius in blocks for Flux devices")
                .defineInRange("scanRadius", 64, 5, 256);
        SPEC = BUILDER.build();
    }
}
