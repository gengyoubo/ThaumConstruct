package github.com.gengyoubo.TCt.client;

import com.mojang.blaze3d.vertex.PoseStack;
import github.com.gengyoubo.TCt.stats.WandCapMaterials;
import github.com.gengyoubo.TCt.item.WandItem;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 仿照神秘时代 4 的 ModelWand 绘制法杖：2x18x2 的杖柄，两端各一个 2x2x2 的杖端（横向放大 1.2 倍）。
 * 贴图使用 TC4 的 32x32 立方体展开布局，杖柄 UV 起点 (0,8)，杖端 UV 起点 (0,0)。
 * 各显示场景的旋转和缩放写在 assets/tc_t/models/item/wand.json 的 display 里。
 */
public class WandRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float TEX = 32f;

    public WandRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        MaterialNBT materials = stack.getItem() instanceof WandItem && ToolStack.isInitialized(stack)
                ? WandItem.toolWithAspectStats(stack).getMaterials() : ToolStack.from(stack).getMaterials();
        ResourceLocation rod = WandTextures.rod(material(materials, 0));
        int cap = stack.getItem() instanceof WandItem wand ? wand.firstCapIndex() : 1;
        ResourceLocation capTop = WandTextures.cap(material(materials, cap));
        ResourceLocation capBottom = WandTextures.cap(material(materials, cap + 1));

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // 让法杖沿原版工具的对角线（左下到右上）摆放，这样能直接套用原版手持物品的显示变换
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        pose.scale(1 / 16f, 1 / 16f, 1 / 16f);
        if (stack.getItem() instanceof WandItem wand && wand.kind() == WandItem.Kind.STAFF) pose.scale(1f, 1.5f, 1f);

        box(buffers.getBuffer(RenderType.entityCutoutNoCull(rod)), pose, -1, -9, -1, 2, 18, 2, 0, 8, light, overlay, capColor(material(materials, 0)));
        pose.pushPose();
        pose.scale(1.2f, 1f, 1.2f);
        box(buffers.getBuffer(RenderType.entityCutoutNoCull(capTop)), pose, -1, 9, -1, 2, 2, 2, 0, 0, light, overlay, capColor(material(materials, cap)));
        box(buffers.getBuffer(RenderType.entityCutoutNoCull(capBottom)), pose, -1, -11, -1, 2, 2, 2, 0, 0, light, overlay, capColor(material(materials, cap + 1)));
        pose.popPose();

        pose.popPose();
    }

    private static MaterialVariantId material(MaterialNBT materials, int index) {
        return index < materials.size() ? materials.get(index).getVariant() : null;
    }

    private static int capColor(MaterialVariantId material) {
        if (material == null || !WandCapMaterials.CUSTOM.contains(material.getId().toString())) return 0xffffffff;
        return MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material)
                .map(info -> info.vertexColor()).orElse(0xffffffff);
    }

    /** 按原版 ModelPart 的 UV 展开方式绘制一个长方体，Y 轴向上，贴图上沿对应长方体顶端。 */
    private static void box(VertexConsumer vc, PoseStack pose, float x, float y, float z, int w, int h, int d,
                            int u, int v, int light, int overlay, int color) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float x1 = x + w, y1 = y + h, z1 = z + d;
        float vs = v + d, ve = v + d + h;
        // 北 (-Z)
        quad(vc, m, n, 0, 0, -1, x1, y1, z, x1, y, z, x, y, z, x, y1, z, u + d, vs, u + d + w, ve, light, overlay, color);
        // 东 (+X)
        quad(vc, m, n, 1, 0, 0, x1, y1, z1, x1, y, z1, x1, y, z, x1, y1, z, u + d + w, vs, u + d + w + d, ve, light, overlay, color);
        // 南 (+Z)
        quad(vc, m, n, 0, 0, 1, x, y1, z1, x, y, z1, x1, y, z1, x1, y1, z1, u + d + w + d, vs, u + 2 * d + 2 * w, ve, light, overlay, color);
        // 西 (-X)
        quad(vc, m, n, -1, 0, 0, x, y1, z, x, y, z, x, y, z1, x, y1, z1, u, vs, u + d, ve, light, overlay, color);
        // 上 (+Y)
        quad(vc, m, n, 0, 1, 0, x, y1, z, x, y1, z1, x1, y1, z1, x1, y1, z, u + d, v, u + d + w, v + d, light, overlay, color);
        // 下 (-Y)
        quad(vc, m, n, 0, -1, 0, x1, y, z, x1, y, z1, x, y, z1, x, y, z, u + d + w, v, u + d + 2 * w, v + d, light, overlay, color);
    }

    /** 四个顶点按从外侧看的 左上、左下、右下、右上 顺序给出。 */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, int light, int overlay, int color) {
        vertex(vc, m, n, ax, ay, az, u0, v0, nx, ny, nz, light, overlay, color);
        vertex(vc, m, n, bx, by, bz, u0, v1, nx, ny, nz, light, overlay, color);
        vertex(vc, m, n, cx, cy, cz, u1, v1, nx, ny, nz, light, overlay, color);
        vertex(vc, m, n, dx, dy, dz, u1, v0, nx, ny, nz, light, overlay, color);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light, int overlay, int color) {
        vc.vertex(m, x, y, z).color((color >> 16) & 255, (color >> 8) & 255, color & 255, (color >>> 24) & 255).uv(u / TEX, v / TEX).overlayCoords(overlay).uv2(light)
                .normal(n, nx, ny, nz).endVertex();
    }
}
