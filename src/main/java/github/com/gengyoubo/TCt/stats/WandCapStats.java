package github.com.gengyoubo.TCt.stats;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 杖端材料属性：魔力消耗倍率。和 TC4 一样，可以给部分要素单独设置倍率
 * （例如 TC4 铜杖端：整体 110%，秩序和混沌 100%）。
 */
public record WandCapStats(float visCost, List<String> specialAspects, float specialCost, int capacityBonus) implements IMaterialStats {
    public static final MaterialStatsId ID = new MaterialStatsId(TCt.getResource("wand_cap"));
    public static final MaterialStatType<WandCapStats> TYPE = new MaterialStatType<>(ID, new WandCapStats(1, List.of(), 1, 0), RecordLoadable.create(
            FloatLoadable.FROM_ZERO.defaultField("vis_cost", 1f, true, WandCapStats::visCost),
            StringLoadable.DEFAULT.list(0).defaultField("special_aspects", List.of(), false, WandCapStats::specialAspects),
            FloatLoadable.FROM_ZERO.defaultField("special_cost", 1f, false, WandCapStats::specialCost),
            IntLoadable.FROM_ZERO.defaultField("capacity_bonus", 0, false, WandCapStats::capacityBonus),
            WandCapStats::new));

    private static final List<Component> DESCRIPTION = List.of(Component.translatable("stat.tc_t.wand_cap.vis_cost.description"));

    public float costFor(Aspect aspect) {
        return specialAspects.contains(aspect.getId()) ? specialCost : visCost;
    }

    @Override
    public MaterialStatType<?> getType() {
        return TYPE;
    }

    @Override
    public List<Component> getLocalizedInfo() {
        List<Component> info = new ArrayList<>();
        info.add(Component.translatable("stat.tc_t.wand_cap.vis_cost", percent(visCost)));
        if (capacityBonus > 0) info.add(Component.translatable("stat.tc_t.wand_cap.capacity_bonus", capacityBonus / 2));
        if (!specialAspects.isEmpty()) {
            MutableComponent aspects = Component.empty();
            for (Aspect aspect : Aspect.values()) {
                if (specialAspects.contains(aspect.getId())) {
                    aspects.append(Component.translatable("aspect.tc_t." + aspect.getId())
                            .withStyle(style -> style.withColor(aspect.getColor()))).append(" ");
                }
            }
            info.add(Component.translatable("stat.tc_t.wand_cap.special_cost", aspects, percent(specialCost)));
        }
        return info;
    }

    @Override
    public List<Component> getLocalizedDescriptions() {
        List<Component> descriptions = new ArrayList<>(DESCRIPTION);
        if (capacityBonus > 0) descriptions.add(Component.translatable("stat.tc_t.wand_cap.capacity_bonus.description"));
        if (!specialAspects.isEmpty()) descriptions.add(Component.empty());
        return descriptions;
    }

    @Override
    public void apply(ModifierStatsBuilder builder, float scale) {
        WandToolStats.VIS_CAPACITY.update(builder, capacityBonus * scale);
        for (Aspect aspect : Aspect.values()) {
            WandToolStats.ASPECT_CAPACITY.get(aspect).update(builder, capacityBonus * scale);
            WandToolStats.VIS_COST.get(aspect).update(builder, costFor(aspect) * scale);
        }
    }

    public static String percent(float cost) {
        return Math.round(cost * 100) + "%";
    }
}
