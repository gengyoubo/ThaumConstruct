package github.com.gengyoubo.TCt.compat.jei;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.arcane.StaffRodCasting;
import github.com.gengyoubo.TCt.item.TCtItems;
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
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

public final class StaffRodRecipeCategory implements IRecipeCategory<StaffRodRecipeCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create(TCt.MOD_ID, "staff_rod_awakening", Recipe.class);
    private final IDrawable icon;
    private final IDrawable arrow;

    public record Recipe(ItemStack blank, ItemStack result) { }

    public StaffRodRecipeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(TinkerSmeltery.searedTable);
        arrow = gui.getRecipeArrow();
    }

    @Override public RecipeType<Recipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.tc_t.staff_rod.title"); }
    @Override public int getWidth() { return 164; }
    @Override public int getHeight() { return 82; }
    @Override public IDrawable getIcon() { return icon; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(5, 18).setStandardSlotBackground().addItemStack(recipe.blank());
        builder.addInputSlot(39, 18).setStandardSlotBackground().addItemStacks(StaffRodCasting.displayCharms());
        var wood = new MaterialId("tconstruct:wood");
        var iron = new MaterialId("tconstruct:iron");
        ItemStack wand = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(MaterialVariant.of((MaterialVariantId) wood), MaterialVariant.of((MaterialVariantId) iron), MaterialVariant.of((MaterialVariantId) iron)));
        builder.addSlot(RecipeIngredientRole.CATALYST, 73, 18).setStandardSlotBackground().addItemStack(wand)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.tc_t.staff_rod.wand")));
        builder.addOutputSlot(139, 18).setOutputSlotBackground().addItemStack(recipe.result());
    }

    @Override public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "1", 44, 5, 0x404040, false);
        graphics.drawString(font, "2", 78, 5, 0x404040, false);
        arrow.draw(graphics, 105, 18);
        graphics.drawString(font, Component.translatable("jei.tc_t.staff_rod.step1"), 3, 43, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.tc_t.staff_rod.step2"), 3, 54, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.tc_t.staff_rod.cost", StaffRodCasting.orderCost(recipe.blank()) / 100), 3, 67, 0x404040, false);
    }
}
