package github.com.gengyoubo.TCt.arcane;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationContainer;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.TinkerStationContainerWrapper;

import java.lang.reflect.Field;

/** The usual Tinkers wand-building recipe, with advanced parts restricted to the arcane anvil. */
public final class ArcaneWandRecipe extends ToolBuildingRecipe {
    private static final Field STATION = stationField();
    private static Field stationField() {
        try {
            Field field = TinkerStationContainerWrapper.class.getDeclaredField("station");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot inspect Tinkers station", e); }
    }
    public ArcaneWandRecipe(ToolBuildingRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.getOutput(), recipe.getOutputCount(),
                recipe.getLayoutSlotId(), recipe.getExtraRequirements());
    }
    @Override public RecipeSerializer<?> getSerializer() { return ArcaneAnvil.WAND_RECIPE.get(); }
    @Override public boolean matches(ITinkerStationContainer container, Level level) {
        if (!super.matches(container, level)) return false;
        boolean advanced = false;
        for (int i = 0; i < container.getInputCount(); i++) {
            var input = container.getInput(i);
            if (input.isEmpty()) continue;
            var material = IMaterialItem.getMaterialFromStack(input);
            if (material != null && ArcaneAnvilBlockEntity.advancedMaterial(material.getId().toString())) advanced = true;
        }
        if (!advanced) return true;
        try {
            return container instanceof TinkerStationContainerWrapper && STATION.get(container) instanceof ArcaneAnvilBlockEntity;
        } catch (IllegalAccessException e) { throw new IllegalStateException("Cannot inspect Tinkers station", e); }
    }

    public static final class Serializer implements RecipeSerializer<ArcaneWandRecipe> {
        @Override public ArcaneWandRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ArcaneWandRecipe(TinkerTables.toolBuildingRecipeSerializer.get().fromJson(id, json));
        }
        @Override public ArcaneWandRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ArcaneWandRecipe(TinkerTables.toolBuildingRecipeSerializer.get().fromNetwork(id, buffer));
        }
        @Override public void toNetwork(FriendlyByteBuf buffer, ArcaneWandRecipe recipe) {
            TinkerTables.toolBuildingRecipeSerializer.get().toNetwork(buffer, recipe);
        }
    }
}
