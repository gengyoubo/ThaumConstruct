package github.com.gengyoubo.TCt.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import slimeknights.tconstruct.library.client.item.ModifiableItemClientExtension;

/** 保留匠魂工具的手部动画，额外提供法杖的 3D 渲染器。 */
public class WandClientExtension extends ModifiableItemClientExtension {
    public static final WandClientExtension INSTANCE = new WandClientExtension();

    private WandRenderer renderer;

    private WandClientExtension() {}

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new WandRenderer();
        }
        return renderer;
    }
}
