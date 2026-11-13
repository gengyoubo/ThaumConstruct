package github.com.gengyoubo.TCt.stats;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.materials.stats.IMaterialStats;
import slimeknights.tconstruct.library.materials.stats.MaterialStatType;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.List;

/** 杖柄材料属性：魔力容量（TC4 木杖柄为 25）。 */
public record WandRodStats(int capacity) implements IMaterialStats {
    public static final MaterialStatsId ID = new MaterialStatsId(TCt.getResource("wand_rod"));
    public static final MaterialStatType<WandRodStats> TYPE = new MaterialStatType<>(ID, new WandRodStats(0), RecordLoadable.create(
            IntLoadable.FROM_ZERO.defaultField("capacity", 0, true, WandRodStats::capacity),
            WandRodStats::new));

    private static final List<Component> DESCRIPTION = List.of(Component.translatable("stat.tc_t.wand_rod.capacity.description"));

    @Override
    public MaterialStatType<?> getType() {
        return TYPE;
    }

    @Override
    public List<Component> getLocalizedInfo() {
        return List.of(Component.translatable("stat.tc_t.wand_rod.capacity", capacity)
                .withStyle(style -> style.withColor(WandToolStats.VIS_CAPACITY.getColor())));
    }

    @Override
    public List<Component> getLocalizedDescriptions() {
        return DESCRIPTION;
    }

    @Override
    public void apply(ModifierStatsBuilder builder, float scale) {
        WandToolStats.VIS_CAPACITY.update(builder, capacity * scale);
    }
}
