package com.fuelingimagination.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArtisanConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue MAX_CRAFTS_PER_BATCH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("artisanCrafting");
        MAX_CRAFTS_PER_BATCH = builder
                .comment("Safety ceiling per lever pull, including at 100% retention or for reusable ingredients.",
                        "Crafting normally stops sooner when the selected recipe no longer matches.")
                .defineInRange("maxCraftsPerBatch", 4096, 1, 65536);
        builder.pop();
        SPEC = builder.build();
    }

    private ArtisanConfig() { }
}
