package github.com.gengyoubo.TCt.stats;

import github.com.gengyoubo.TCt.TCt;
import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

import java.util.EnumMap;
import java.util.Map;

/** 法杖的工具属性：魔力容量（来自杖柄）和每种要素的魔力消耗倍率（两个杖端各占一半）。 */
public final class WandToolStats {
    private WandToolStats() {}

    /** 每种要素可储存的魔力上限 */
    public static final FloatToolStat VIS_CAPACITY =
            ToolStats.register(new FloatToolStat(new ToolStatId(TCt.MOD_ID, "vis_capacity"), 0xFFAA00, 0, 0, 10000));

    /** 每种要素的魔力消耗倍率，1.1 即 110% */
    public static final Map<Aspect, FloatToolStat> VIS_COST = new EnumMap<>(Aspect.class);

    static {
        for (Aspect aspect : Aspect.values()) {
            VIS_COST.put(aspect, ToolStats.register(new FloatToolStat(
                    new ToolStatId(TCt.MOD_ID, "vis_cost_" + aspect.getId()), aspect.getColor().getValue(), 0, 0, 10)));  // 默认 0：两个杖端各贡献一半，合起来就是平均值
        }
    }

    /** 触发静态初始化，必须在加载数据之前调用 */
    public static void init() {}

    public static float averageCost(slimeknights.tconstruct.library.tools.nbt.StatsNBT stats) {
        float total = 0;
        for (FloatToolStat stat : VIS_COST.values()) {
            total += stats.get(stat);
        }
        return total / VIS_COST.size();
    }
}
