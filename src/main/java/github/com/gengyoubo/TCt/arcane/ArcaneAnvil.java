package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;

public final class ArcaneAnvil {
    private ArcaneAnvil() { }
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TCt.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TCt.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TCt.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TCt.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, TCt.MOD_ID);
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("arcane_anvil", () -> new ArcaneAnvilBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> ITEM = ITEMS.register("arcane_anvil", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<ArcaneAnvilBlockEntity>> TILE = TILES.register("arcane_anvil", () -> BlockEntityType.Builder.of(ArcaneAnvilBlockEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<TinkerStationContainerMenu>> MENU = MENUS.register("arcane_anvil", () -> IForgeMenuType.create(ArcaneAnvilMenu::new));
    public static final RegistryObject<RecipeSerializer<ArcaneWandRecipe>> WAND_RECIPE = RECIPES.register("wand_building", ArcaneWandRecipe.Serializer::new);
    public static void register(IEventBus bus) { BLOCKS.register(bus); ITEMS.register(bus); TILES.register(bus); MENUS.register(bus); RECIPES.register(bus); }
}
