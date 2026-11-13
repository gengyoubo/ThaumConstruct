package github.com.gengyoubo.TCt.arcane;

import net.minecraftforge.common.ForgeConfigSpec;

/** Configurable wand part values; two rod values are multiplied by the cap value. */
public final class ArcaneAnvilConfig {
    private ArcaneAnvilConfig() { }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SILVERWOOD_ROD;
    public static final ForgeConfigSpec.IntValue GOLD_CAP;
    public static final ForgeConfigSpec.IntValue COPPER_CAP;
    public static final ForgeConfigSpec.IntValue STAFF_ROD_ORDER_PER_TIER;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("arcane_wand_crafting");
        SILVERWOOD_ROD = builder.comment("Silverwood rod value; two rod values are used when crafting a wand.").defineInRange("silverwood_rod_value", 4.5, 0.01, 100.0);
        GOLD_CAP = builder.comment("Gold cap crafting cost per aspect.").defineInRange("gold_cap", 3, 1, 100);
        COPPER_CAP = builder.comment("Copper cap crafting cost per aspect.").defineInRange("copper_cap", 2, 1, 100);
        STAFF_ROD_ORDER_PER_TIER = builder.comment("Order vis used to awaken a cast staff core, multiplied by its Tinkers material tier.")
                .defineInRange("staff_rod_order_per_tier", 10, 1, 1000);
        builder.pop();
        SPEC = builder.build();
    }
}
