package org.chatterjay.visible_energy.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class VEConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue SCAN_RADIUS;
    public static final ModConfigSpec.IntValue DISPLAY_DURATION;
    public static final ModConfigSpec.EnumValue<RenderStyle> RENDER_STYLE;

    public static final ModConfigSpec SPEC;

    public enum RenderStyle {
        OUTLINE,
        BEAM,
        BOTH
    }

    static {
        BUILDER.push("scanning");
        SCAN_RADIUS = BUILDER
                .comment("Radius in blocks to scan for Flux devices")
                .defineInRange("scanRadius", 32, 5, 128);
        BUILDER.pop();

        BUILDER.push("display");
        DISPLAY_DURATION = BUILDER
                .comment("Duration in seconds to display highlights")
                .defineInRange("displayDuration", 30, 5, 300);
        RENDER_STYLE = BUILDER
                .comment("Highlight render style: OUTLINE, BEAM, or BOTH")
                .defineEnum("renderStyle", RenderStyle.BOTH);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
