package github.com.gengyoubo.TCt.stats;

import java.util.Set;

/** Materials whose wand models use their Tinkers material color. */
public final class WandCapMaterials {
    private WandCapMaterials() { }
    public static final Set<String> CUSTOM = Set.of(
            "tconstruct:cobalt", "tconstruct:steel", "tconstruct:cinderslime", "tconstruct:queens_slime",
            "tconstruct:hepatizon", "tconstruct:manyullyn", "tconstruct:blazing_bone", "tconstruct:nicrosil",
            "tconstruct:knightmetal", "tconstruct:knightslime", "tc_t:diamond", "tc_t:netherite");
}
