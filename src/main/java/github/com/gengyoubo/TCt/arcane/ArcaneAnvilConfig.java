package github.com.gengyoubo.TCt.arcane;

import net.minecraftforge.common.ForgeConfigSpec;

/** TC4 wand-crafting costs; the product is charged for each primal aspect. */
public final class ArcaneAnvilConfig {
    private ArcaneAnvilConfig() { }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue SILVERWOOD_ROD;
    public static final ForgeConfigSpec.IntValue GOLD_CAP;
    public static final ForgeConfigSpec.IntValue COPPER_CAP;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("arcane_wand_crafting");
        SILVERWOOD_ROD = builder.comment("Silverwood rod crafting multiplier.").defineInRange("silverwood_rod", 9, 1, 100);
        GOLD_CAP = builder.comment("Gold cap crafting multiplier.").defineInRange("gold_cap", 3, 1, 100);
        COPPER_CAP = builder.comment("Copper cap crafting multiplier.").defineInRange("copper_cap", 2, 1, 100);
        builder.pop();
        SPEC = builder.build();
    }
}
