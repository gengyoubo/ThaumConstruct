package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.compat.tc4.ArcaneVis;
import github.com.gengyoubo.TCt.item.TCtItems;
import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.mfcl.api.LegacyMappings;
import github.com.gengyoubo.mfcl.api.LegacyStack;
import github.com.gengyoubo.mfcl.api.MfclApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.MinecraftForge;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.inventory.TinkerStationContainerWrapper;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;

import java.util.EnumMap;

@GameTestHolder(TCt.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArcaneAnvilGameTests {
    private ArcaneAnvilGameTests() { }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void persistenceAndCost(GameTestHelper helper) {
        BlockPos local = new BlockPos(0, 1, 0);
        helper.setBlock(local, ArcaneAnvil.BLOCK.get());
        if (!(helper.getBlockEntity(local) instanceof ArcaneAnvilBlockEntity tile)) throw new AssertionError("Missing arcane anvil block entity");
        tile.setItem(1, new ItemStack(Items.DIAMOND, 3));
        tile.wandSlot().setItem(0, new ItemStack(Items.STICK));
        var saved = tile.saveWithFullMetadata();
        BlockEntity restored = BlockEntity.loadStatic(tile.getBlockPos(), tile.getBlockState(), saved);
        if (!(restored instanceof ArcaneAnvilBlockEntity copy) || copy.getItem(1).getCount() != 3
                || !copy.wandSlot().getItem(0).is(Items.STICK)) throw new AssertionError("Anvil inventories did not survive save/load");
        var material = MaterialNBT.of(MaterialVariant.of((MaterialVariantId) new MaterialId("tc_t:silverwood")),
                MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:gold")), MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:gold")));
        var built = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), material);
        var cost = ArcaneAnvilBlockEntity.cost(built);
        if (cost.size() != 6 || cost.values().stream().anyMatch(value -> value != 2700)) throw new AssertionError("Gold silverwood recipe cost is incorrect: " + cost);
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void visPayment(GameTestHelper helper) {
        var runtime = MfclApi.runtime().orElseThrow();
        var legacy = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                "{rod:\"wood\",cap:\"iron\",aer:2500,terra:2500,ignis:2500,aqua:2500,ordo:2500,perditio:2500}"));
        ItemStack wand = runtime.mappings().toHost(LegacyMappings.ITEM_STACK, legacy, ItemStack.class).orElseThrow();
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var costs = new EnumMap<Aspect, Integer>(Aspect.class);
        for (var aspect : Aspect.values()) costs.put(aspect, 100);
        var simulated = ArcaneVis.pay(player, wand, costs, true).orElseThrow();
        if (!ItemStack.matches(wand, simulated)) throw new AssertionError("Simulated payment changed the wand");
        var paid = ArcaneVis.pay(player, wand, costs, false).orElseThrow();
        if (ItemStack.matches(wand, paid)) throw new AssertionError("Payment did not change the wand");
        for (var aspect : Aspect.values()) costs.put(aspect, 100000);
        if (ArcaneVis.pay(player, wand, costs, false).isPresent()) throw new AssertionError("Insufficient vis accepted");
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void advancedWandCraft(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var silverwood = new MaterialId("tc_t:silverwood");
        var gold = new MaterialId("tconstruct:gold");
        var rod = TCtItems.WAND_ROD.get().withMaterial(silverwood);
        var cap = TCtItems.WAND_CAP.get().withMaterial(gold);
        if (rod.isEmpty() || cap.isEmpty()) throw new AssertionError("Advanced wand parts are unavailable");
        BlockPos arcanePos = new BlockPos(0, 1, 0), ordinaryPos = new BlockPos(1, 1, 0);
        helper.setBlock(arcanePos, ArcaneAnvil.BLOCK.get());
        helper.setBlock(ordinaryPos, TinkerTables.tinkersAnvil.get());
        var tile = (ArcaneAnvilBlockEntity) helper.getBlockEntity(arcanePos);
        var ordinary = (TinkerStationBlockEntity) helper.getBlockEntity(ordinaryPos);
        for (var station : new TinkerStationBlockEntity[]{tile, ordinary}) {
            station.setItem(1, rod.copy()); station.setItem(2, cap.copy()); station.setItem(3, cap.copy());
        }
        var recipe = level.getRecipeManager().byKey(TCt.getResource("tools/building/wand")).orElseThrow();
        if (!(recipe instanceof ArcaneWandRecipe wandRecipe)) throw new AssertionError("Wand recipe did not load");
        if (wandRecipe.matches(new TinkerStationContainerWrapper(ordinary), level)) throw new AssertionError("Ordinary anvil accepts advanced parts");
        if (!wandRecipe.matches(new TinkerStationContainerWrapper(tile), level)) throw new AssertionError("Arcane anvil rejected advanced parts");
        if (!tile.calcResult(player).isEmpty()) throw new AssertionError("Output allowed without a wand");
        var menu = new ArcaneAnvilMenu(1, player.getInventory(), tile);
        if (menu.getType() != ArcaneAnvil.MENU.get()) throw new AssertionError("Wrong arcane menu type");
        menu.setToolSelection(StationSlotLayout.EMPTY);
        if (!menu.wandSlot().isActive() || !menu.getSlot(1).mayPlace(rod))
            throw new AssertionError("Non-arcane selection replaced the wand layout");
        if (tile.canPay(player, new ItemStack(Items.DIAMOND_SWORD)))
            throw new AssertionError("Non-wand output was allowed");
        var output = menu.slots.stream().filter(slot -> slot instanceof LazyResultSlot).findFirst().orElseThrow();
        if (output.mayPickup(player)) throw new AssertionError("Output slot allowed pickup without vis");
        var runtime = MfclApi.runtime().orElseThrow();
        var old = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                "{rod:\"silverwood\",cap:\"iron\",aer:10000,terra:10000,ignis:10000,aqua:10000,ordo:10000,perditio:10000}"));
        tile.wandSlot().setItem(0, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, old, ItemStack.class).orElseThrow());
        var result = tile.calcResult(player);
        if (result.isEmpty()) throw new AssertionError("Charged wand did not enable output");
        if (!output.mayPickup(player)) throw new AssertionError("Output slot rejected a charged wand");
        ItemStack before = tile.wandSlot().getItem(0).copy();
        ItemStack taken = output.remove(1);
        if (taken.isEmpty()) throw new AssertionError("Output slot produced no wand");
        output.onTake(player, taken);
        if (ItemStack.matches(before, tile.wandSlot().getItem(0))) throw new AssertionError("Craft did not charge the wand");
        if (!tile.getItem(1).isEmpty() || !tile.getItem(2).isEmpty() || !tile.getItem(3).isEmpty()) throw new AssertionError("Craft did not consume parts");
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void wandConvertsAnvil(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos local = new BlockPos(0, 1, 0);
        helper.setBlock(local, TinkerTables.tinkersAnvil.get());
        var old = (TinkerStationBlockEntity) helper.getBlockEntity(local);
        old.setItem(1, new ItemStack(Items.DIAMOND, 3));
        old.updateTexture("minecraft:gold_block");
        var runtime = MfclApi.runtime().orElseThrow();
        var stack = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1, "{rod:\"wood\",cap:\"iron\"}"));
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, stack, ItemStack.class).orElseThrow());
        BlockPos pos = helper.absolutePos(local);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.setShiftKeyDown(false);
        var normalClick = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        MinecraftForge.EVENT_BUS.post(normalClick);
        if (normalClick.isCanceled() || level.getBlockEntity(pos) != old) throw new AssertionError("Normal right click converted the anvil");
        player.setShiftKeyDown(true);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        MinecraftForge.EVENT_BUS.post(event);
        player.setShiftKeyDown(false);
        if (!event.isCanceled() || !(level.getBlockEntity(pos) instanceof ArcaneAnvilBlockEntity anvil)
                || anvil.getItem(1).getCount() != 3 || !"minecraft:gold_block".equals(anvil.getTextureName())) throw new AssertionError("Conversion lost the original inventory or material");
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void tinkersWandConvertsAnvil(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos local = new BlockPos(0, 1, 0);
        helper.setBlock(local, TinkerTables.tinkersAnvil.get());
        var material = MaterialNBT.of(MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:wood")),
                MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:iron")),
                MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:iron")));
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), material));
        BlockPos pos = helper.absolutePos(local);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        player.setShiftKeyDown(true);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
        MinecraftForge.EVENT_BUS.post(event);
        player.setShiftKeyDown(false);
        if (!event.isCanceled() || !(level.getBlockEntity(pos) instanceof ArcaneAnvilBlockEntity))
            throw new AssertionError("Tinkers wand did not convert the anvil");
        helper.succeed();
    }
}
