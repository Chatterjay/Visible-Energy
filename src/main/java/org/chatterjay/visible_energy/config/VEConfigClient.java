package org.chatterjay.visible_energy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VEConfigClient {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue DISPLAY_DURATION;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("display");
        DISPLAY_DURATION = BUILDER
                .comment("Duration in seconds to display highlights")
                .defineInRange("displayDuration", 30, 5, 300);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
