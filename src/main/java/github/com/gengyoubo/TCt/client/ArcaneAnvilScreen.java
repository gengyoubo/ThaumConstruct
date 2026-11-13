package github.com.gengyoubo.TCt.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvilBlockEntity;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvilMenu;
import github.com.gengyoubo.TCt.stats.Aspect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.tables.client.inventory.widget.TinkerStationButtonsWidget;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;
import slimeknights.tconstruct.tables.menu.slot.TinkerStationSlot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static github.com.gengyoubo.TCt.arcane.ArcaneAnvilLayout.*;

/**
 * Tinker station screen laid out like the TC4 arcane workbench: the material slots sit on the TC4 cloth with the
 * primal aspects on its rune circles, and the TC4 wand box sits next to the original arrow and result slot.
 * TC4 sprites are read from the TC4 jar at runtime; everything else is pieces of Tinkers' own tinker.png.
 */
public final class ArcaneAnvilScreen extends TinkerStationScreen {
    private static final ResourceLocation TINKER_TEXTURE = new ResourceLocation("tconstruct", "textures/gui/tinker.png");
    private static final ResourceLocation WAND_LAYOUT = TCt.getResource("wand");
    private static final String TC4_GUI = "gui_arcaneworkbench";
    // Keep the TC4 grid cells under the rod and caps ({column, row}).
    private static final UnaryOperator<NativeImage> WAND_CLOTH = Tc4GridEraser.keeping(new int[]{2, 0}, new int[]{1, 1}, new int[]{0, 2});
    private static final UnaryOperator<NativeImage> SCEPTRE_CLOTH = Tc4GridEraser.keeping(
            new int[]{1, 0}, new int[]{2, 0}, new int[]{1, 1}, new int[]{2, 1}, new int[]{0, 2});
    /** TC4 aspect positions (centres) in gui_arcaneworkbench.png, in TC4's primal order: aer, terra, ignis, aqua, ordo, perditio. */
    private static final int[][] TC4_ASPECTS = {{72, 21}, {24, 43}, {24, 102}, {72, 124}, {120, 102}, {120, 43}};

    public ArcaneAnvilScreen(TinkerStationContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
        inventoryLabelY += INVENTORY_SHIFT;
    }

    @Override public void init() {
        super.init();
        StationSlotLayout wand = StationSlotLayoutLoader.getInstance().get(WAND_LAYOUT);
        // Restrict the tool selector to the three Thaumcraft tool layouts.
        if (!isWandLayout()) super.onToolSelection(wand);
        var sceptre = StationSlotLayoutLoader.getInstance().get(TCt.getResource("sceptre"));
        var staff = StationSlotLayoutLoader.getInstance().get(TCt.getResource("staff"));
        buttonsScreen = new TinkerStationButtonsWidget(this,
                cornerX - TinkerStationButtonsWidget.width(3) - 2,
                cornerY + centerBeam.h + buttonDecorationTop.h,
                List.of(wand, sceptre, staff), TinkerStationButtonsWidget.METAL_STYLE) {
            @Override public int getImageWidth() { return TinkerStationButtonsWidget.width(3); }
        };
    }

    private boolean isWandLayout() {
        var id = getCurrentLayout().getName();
        return WAND_LAYOUT.equals(id) || TCt.getResource("sceptre").equals(id) || TCt.getResource("staff").equals(id);
    }

    @Override public void updateLayout() {
        super.updateLayout();
        if (menu instanceof ArcaneAnvilMenu arcane) arcane.setWandView(isWandLayout());
    }

    @Override public void onToolSelection(StationSlotLayout layout) {
        var id = layout.getName();
        if (WAND_LAYOUT.equals(id) || TCt.getResource("sceptre").equals(id) || TCt.getResource("staff").equals(id)) super.onToolSelection(layout);
    }

    /** Builds the bigger panel from pieces of tinker.png: border, plain fill, and the original slot frames and arrow. */
    @Override protected void drawBackground(GuiGraphics graphics, ResourceLocation texture) {
        if (!isWandLayout() || !TINKER_TEXTURE.equals(texture)) {
            super.drawBackground(graphics, texture);
            return;
        }
        int x = cornerX, y = cornerY, w = realWidth, h = realHeight;
        graphics.setColor(1, 1, 1, 1);
        // 9-slice panel: 3px borders, corners from the original 176x184 panel
        graphics.blit(texture, x, y, 0, 0, 3, 3);
        graphics.blit(texture, x + w - 3, y, 173, 0, 3, 3);
        graphics.blit(texture, x, y + h - 3, 0, 181, 3, 3);
        graphics.blit(texture, x + w - 3, y + h - 3, 173, 181, 3, 3);
        graphics.blit(texture, x + 3, y, w - 6, 3, 3, 0, 1, 3, 256, 256);
        graphics.blit(texture, x + 3, y + h - 3, w - 6, 3, 3, 181, 1, 3, 256, 256);
        graphics.blit(texture, x, y + 3, 3, h - 6, 0, 3, 3, 1, 256, 256);
        graphics.blit(texture, x + w - 3, y + 3, 3, h - 6, 173, 3, 3, 1, 256, 256);
        graphics.blit(texture, x + 3, y + 3, w - 6, h - 6, 10, 10, 1, 1, 256, 256);
        // player inventory, armor, result and arrow, copied from their original spots
        graphics.blit(texture, x + 7, y + 101 + INVENTORY_SHIFT, 7, 101, 162, 76);
        for (int armorY : ARMOR_Y) slotFrame(graphics, texture, ARMOR_X, armorY);
        graphics.blit(texture, x + RESULT_X - 5, y + RESULT_Y - 5, 109, 33, 26, 26);
        graphics.blit(texture, x + ARROW_X, y + ARROW_Y, 80, 38, 22, 15);
    }

    private void slotFrame(GuiGraphics graphics, ResourceLocation texture, int slotX, int slotY) {
        graphics.blit(texture, cornerX + slotX - 1, cornerY + slotY - 1, 7, 101, 18, 18);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (!isWandLayout()) return;
        // the parent drew its tool icon and slot frames in the old spot; the cloth goes over them and they are redrawn
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        boolean sceptre = TCt.getResource("sceptre").equals(getCurrentLayout().getName());
        Optional<ResourceLocation> tc4 = sceptre
                ? Tc4Textures.gui(TC4_GUI, TC4_GUI + "_sceptre", SCEPTRE_CLOTH)
                : Tc4Textures.gui(TC4_GUI, TC4_GUI + "_wand", WAND_CLOTH);
        int x = cornerX, y = cornerY;
        if (tc4.isPresent()) {
            graphics.blit(tc4.get(), x + CLOTH_X, y + CLOTH_Y, CLOTH_U, CLOTH_V, CLOTH_W, CLOTH_H);
        } else {
            graphics.fill(x + CLOTH_X, y + CLOTH_Y, x + CLOTH_X + CLOTH_W, y + CLOTH_Y + CLOTH_H, 0xff03461a);
        }
        StationSlotLayout layout = getCurrentLayout();
        // Fall back to ordinary slot frames when TC4 textures are unavailable.
        if (tc4.isEmpty()) drawStationSlots(graphics, layout);
        drawAspects(graphics);

        if (menu instanceof ArcaneAnvilMenu arcane) {
            Slot wand = arcane.wandSlot();
            if (tc4.isPresent()) {
                graphics.blit(tc4.get(), x + wand.x - 3, y + wand.y - 3, 157, 21, 22, 22);
            } else {
                graphics.fill(x + wand.x - 1, y + wand.y - 1, x + wand.x + 17, y + wand.y + 17, 0xff37312c);
            }
        }
        RenderSystem.enableDepthTest();
    }

    /** Plain slot frames for the active station slots, the same frames as the player inventory. */
    private void drawStationSlots(GuiGraphics graphics, StationSlotLayout layout) {
        var menu = getMenu();
        if (!layout.getToolSlot().isHidden()) {
            Slot tool = menu.getSlot(0);
            slotFrame(graphics, TINKER_TEXTURE, tool.x, tool.y);
        }
        for (int i = 1; i <= getMaxInputs(); i++) {
            Slot slot = menu.getSlot(i);
            boolean shown = i <= activeInputs
                    || slot instanceof TinkerStationSlot station && (!station.isDormant() || slot.hasItem());
            if (shown) slotFrame(graphics, TINKER_TEXTURE, slot.x, slot.y);
        }
    }

    /** Vis cost of the result on the six rune circles of the cloth; faint when nothing is needed. */
    private void drawAspects(GuiGraphics graphics) {
        Map<Aspect, Integer> costs = Map.of();
        for (Slot slot : menu.slots) {
            if (slot instanceof LazyResultSlot) {
                ItemStack result = slot.getItem();
                if (!result.isEmpty()) costs = ArcaneAnvilBlockEntity.cost(result);
                break;
            }
        }
        Aspect[] aspects = Aspect.values();
        for (int i = 0; i < aspects.length; i++) {
            Aspect aspect = aspects[i];
            int cx = cornerX + TC4_ASPECTS[i][0] + TC4_DX, cy = cornerY + TC4_ASPECTS[i][1] + TC4_DY;
            int amount = costs.getOrDefault(aspect, 0);
            int rgb = aspect.getColor().getValue();
            float alpha = amount > 0 ? 1f : 0.3f;
            var icon = Tc4Textures.aspect(aspect.getId());
            if (icon.isPresent()) {
                // TC4 aspect icons are greyscale and tinted with the aspect colour
                graphics.setColor(((rgb >> 16) & 0xff) / 255f, ((rgb >> 8) & 0xff) / 255f, (rgb & 0xff) / 255f, alpha);
                graphics.blit(icon.get(), cx - 8, cy - 8, 0, 0, 16, 16, 16, 16);
                graphics.setColor(1, 1, 1, 1);
            } else {
                graphics.fill(cx - 4, cy - 4, cx + 4, cy + 4, (Math.round(alpha * 255) << 24) | rgb);
            }
            if (amount > 0) {
                String text = java.math.BigDecimal.valueOf(amount, 2).stripTrailingZeros().toPlainString();
                graphics.drawString(font, text, cx + 9 - font.width(text), cy + 1, 0xffffffff, true);
            }
        }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isWandLayout() && menu instanceof ArcaneAnvilMenu arcane && !arcane.wandSlot().hasItem()
                && isHovering(arcane.wandSlot(), mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.tc_t.arcane_anvil.wand_slot"), mouseX, mouseY);
        }
    }
}
