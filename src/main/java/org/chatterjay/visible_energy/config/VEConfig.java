package org.chatterjay.visible_energy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VEConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SCAN_RADIUS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("scanning");
        SCAN_RADIUS = BUILDER
                .comment("Radius in blocks to scan for Flux devices")
                .translation("visible_energy.config.scanning.scanRadius")
                .defineInRange("scanRadius", 64, 5, 256);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
