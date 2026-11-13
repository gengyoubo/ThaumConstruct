package github.com.gengyoubo.TCt.stats;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;

/**
 * 法杖部件的材料属性类型。只有在材料属性 JSON 里声明了这些属性的材料才能制作对应部件，
 * 以此限制杖柄、杖端可用的材料。
 */
public enum WandPartStats implements IMaterialStats {
    /** 杖柄：木、石、铜、铁 */
    ROD("wand_rod"),
    /** 杖端：铜、铁 */
    CAP("wand_cap");

    private static final List<Component> LOCALIZED =
            List.of(IMaterialStats.makeTooltip(new ResourceLocation("tconstruct", "extra.no_stats")));
    private static final List<Component> DESCRIPTION = List.of(Component.empty());

    private final MaterialStatType<WandPartStats> type;

    WandPartStats(String name) {
        this.type = MaterialStatType.singleton(new MaterialStatsId(TCt.getResource(name)), this);
    }

    @Override
    public MaterialStatType<WandPartStats> getType() {
        return type;
    }

    @Override
    public List<Component> getLocalizedInfo() {
        return LOCALIZED;
    }

    @Override
    public List<Component> getLocalizedDescriptions() {
        return DESCRIPTION;
    }

    @Override
    public void apply(ModifierStatsBuilder builder, float scale) {}

    /** 注册到匠魂的材料注册表；材料特性沿用近战/采掘类的特性。 */
    public static void registerStatTypes() {
        for (WandPartStats stats : values()) {
            MaterialRegistry.getInstance().registerStatType(stats.getType(), MaterialRegistry.MELEE_HARVEST);
        }
    }
}
