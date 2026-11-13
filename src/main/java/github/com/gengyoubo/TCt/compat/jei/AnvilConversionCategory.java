package github.com.gengyoubo.TCt.compat.jei;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvil;
import github.com.gengyoubo.TCt.arcane.AnvilConversionVariants;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** A placed anvil retains its material when converted with a sneaking wand interaction. */
public final class AnvilConversionCategory implements IRecipeCategory<AnvilConversionCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create(TCt.MOD_ID, "anvil_conversion", Recipe.class);
    private static final ResourceLocation BOOK = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_researchbook.png");
    private static final int WIDTH = 160, HEIGHT = 64, ROW = 26, BLOCK_X = 20, WAND_X = 72, RESULT_X = 124;
    private static final int INK = 0xFF505050;
    private final IDrawable icon;

    public record Recipe(ItemStack source, List<ItemStack> wands, ItemStack result) { }

    public AnvilConversionCategory(IGuiHelper gui) { icon = gui.createDrawableItemLike(ArcaneAnvil.BLOCK.get()); }
    @Override public RecipeType<Recipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.tc_t.anvil_conversion.title"); }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public IDrawable getIcon() { return icon; }

    public static List<Recipe> recipes(List<ItemStack> wands) {
        if (wands.isEmpty()) return List.of();
        var catalysts = List.copyOf(wands);
        return AnvilConversionVariants.all().stream()
                .map(variant -> new Recipe(variant.source(), catalysts, variant.result())).toList();
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, BLOCK_X, ROW).addItemStack(recipe.source());
        builder.addSlot(RecipeIngredientRole.CATALYST, WAND_X, ROW - 20).addItemStacks(recipe.wands())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.tc_t.anvil_conversion.wand")));
        builder.addSlot(RecipeIngredientRole.OUTPUT, RESULT_X, ROW).addItemStack(recipe.result())
                .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable("jei.tc_t.anvil_conversion.material")));
    }

    @Override public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        if (minecraft.getResourceManager().getResource(BOOK).isPresent())
            graphics.blit(BOOK, 0, 0, WIDTH, HEIGHT, 258, 4, 234, 348, 512, 512);
        else for (int x : new int[]{BLOCK_X, WAND_X, RESULT_X}) frame(graphics, x, x == WAND_X ? ROW - 20 : ROW);
        graphics.fill(BLOCK_X + 22, ROW + 7, RESULT_X - 8, ROW + 9, INK);
        graphics.drawString(font, ">", RESULT_X - 9, ROW + 4, INK, false);
        String click = Component.translatable("jei.tc_t.anvil_conversion.click").getString();
        graphics.drawString(font, click, WAND_X + 8 - font.width(click) / 2, ROW + 12, INK, false);
        String note = font.plainSubstrByWidth(Component.translatable("jei.tc_t.anvil_conversion.material").getString(), WIDTH - 8);
        graphics.drawString(font, note, (WIDTH - font.width(note)) / 2, HEIGHT - 12, INK, false);
    }

    private static void frame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
        graphics.fill(x, y, x + 16, y + 16, 0xFFE8E8E8);
    }
}
