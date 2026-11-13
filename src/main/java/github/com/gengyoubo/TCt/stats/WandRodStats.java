package github.com.gengyoubo.TCt.stats;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;
import java.util.ArrayList;

/** 杖柄材料属性：魔力容量（TC4 木杖柄为 25）。 */
public record WandRodStats(int capacity, String specialAspect, int specialBonus, int otherBonus) implements IMaterialStats {
    public static final MaterialStatsId ID = new MaterialStatsId(TCt.getResource("wand_rod"));
    public static final MaterialStatType<WandRodStats> TYPE = new MaterialStatType<>(ID, new WandRodStats(0, "", 0, 0), RecordLoadable.create(
            IntLoadable.FROM_ZERO.defaultField("capacity", 0, true, WandRodStats::capacity),
            StringLoadable.DEFAULT.defaultField("special_aspect", "", false, WandRodStats::specialAspect),
            IntLoadable.ANY_FULL.defaultField("special_bonus", 0, false, WandRodStats::specialBonus),
            IntLoadable.ANY_FULL.defaultField("other_bonus", 0, false, WandRodStats::otherBonus),
            WandRodStats::new));

    private static final List<Component> DESCRIPTION = List.of(Component.translatable("stat.tc_t.wand_rod.capacity.description"));

    @Override
    public MaterialStatType<?> getType() {
        return TYPE;
    }

    @Override
    public List<Component> getLocalizedInfo() {
        List<Component> info = new ArrayList<>();
        info.add(Component.translatable("stat.tc_t.wand_rod.capacity", capacity)
                .withStyle(style -> style.withColor(WandToolStats.VIS_CAPACITY.getColor())));
        if (!specialAspect.isEmpty()) {
            info.add(Component.translatable("stat.tc_t.wand_rod.special_capacity",
                    Component.translatable("aspect.tc_t." + specialAspect), signed(specialBonus), signed(otherBonus)));
        }
        return info;
    }

    @Override
    public List<Component> getLocalizedDescriptions() {
        return specialAspect.isEmpty() ? DESCRIPTION : List.of(DESCRIPTION.get(0), Component.empty());
    }

    @Override
    public void apply(ModifierStatsBuilder builder, float scale) {
        WandToolStats.VIS_CAPACITY.update(builder, capacity * scale);
        for (Aspect aspect : Aspect.values()) {
            int bonus = specialAspect.isEmpty() ? 0 : aspect.getId().equals(specialAspect) ? specialBonus : otherBonus;
            WandToolStats.ASPECT_CAPACITY.get(aspect).update(builder, Math.max(0, capacity + bonus) * scale);
        }
    }

    private static String signed(int value) { return value > 0 ? "+" + value : Integer.toString(value); }
}
