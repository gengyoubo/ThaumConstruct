package github.com.gengyoubo.TCt.compat.jei;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvil;
import github.com.gengyoubo.TCt.arcane.StaffRodCasting;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import github.com.gengyoubo.TCt.item.TCtItems;
import github.com.gengyoubo.TCt.item.WandItem;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.plugin.jei.util.ToolPartSubtypeInterpreter;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

@JeiPlugin
public final class TCtJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid() { return TCt.getResource("jei"); }

    @Override public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Finished parts are registered by Tinkers via its tool-parts tag. Blanks are not tool parts.
        registration.registerSubtypeInterpreter(TCtItems.STAFF_ROD_BLANK.get(), ToolPartSubtypeInterpreter.INSTANCE);
        registration.registerSubtypeInterpreter(ArcaneAnvil.ITEM.get(), (stack, context) -> {
            if (context != UidContext.Ingredient) return "";
            String texture = RetexturedHelper.getTextureName(stack);
            return texture.isEmpty() ? ToolPartSubtypeInterpreter.INSTANCE.apply(stack, context) : "texture:" + texture;
        });
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new StaffRodRecipeCategory(gui), new AnvilConversionCategory(gui));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        var recipes = MaterialRegistry.getInstance().getVisibleMaterials().stream()
                .filter(material -> TCtItems.STAFF_ROD.get().canUseMaterial(material))
                .filter(material -> TCtItems.STAFF_ROD_BLANK.get().canUseMaterial(material))
                .map(material -> new StaffRodRecipeCategory.Recipe(
                        TCtItems.STAFF_ROD_BLANK.get().withMaterial(material.getIdentifier()),
                        TCtItems.STAFF_ROD.get().withMaterial(material.getIdentifier())))
                .filter(recipe -> !recipe.blank().isEmpty() && !recipe.result().isEmpty())
                .toList();
        registration.addRecipes(StaffRodRecipeCategory.TYPE, recipes);
        var wands = registration.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK).stream()
                .filter(stack -> {
                    var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                    return stack.getItem() instanceof WandItem
                            || (id.getNamespace().equals("thaumcraft") && id.getPath().contains("wand"));
                })
                .filter(Tc4WandBridge::isHostWand)
                .map(stack -> stack.copyWithCount(1)).toList();
        registration.addRecipes(AnvilConversionCategory.TYPE, AnvilConversionCategory.recipes(wands));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(StaffRodRecipeCategory.TYPE, TinkerSmeltery.searedTable, TinkerSmeltery.scorchedTable);
    }
}
