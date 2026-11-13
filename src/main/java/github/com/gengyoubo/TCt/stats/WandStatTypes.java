package github.com.gengyoubo.TCt.stats;

import slimeknights.tconstruct.library.materials.MaterialRegistry;

/** 把杖柄、杖端的材料属性类型注册到匠魂。不设特性回退，材料特性由数据文件单独控制。 */
public final class WandStatTypes {
    private WandStatTypes() {}

    public static void register() {
        MaterialRegistry.getInstance().registerStatType(WandRodStats.TYPE);
        MaterialRegistry.getInstance().registerStatType(WandCapStats.TYPE);
    }
}
