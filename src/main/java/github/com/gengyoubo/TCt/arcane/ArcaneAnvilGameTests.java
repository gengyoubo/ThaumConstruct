package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.compat.tc4.ArcaneVis;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import github.com.gengyoubo.TCt.item.WandItem;
import github.com.gengyoubo.TCt.item.TCtItems;
import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.TCt.stats.WandToolStats;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
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
        var goldWood = MaterialNBT.of(MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:wood")),
                MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:gold")), MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:gold")));
        var goldWoodCost = ArcaneAnvilBlockEntity.cost(ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), goldWood));
        if (goldWoodCost.size() != 6 || goldWoodCost.values().stream().anyMatch(value -> value != 600))
            throw new AssertionError("Gold wood recipe cost is incorrect: " + goldWoodCost);
        var cobalt = MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:cobalt"));
        var diamond = MaterialVariant.of((MaterialVariantId) new MaterialId("tc_t:diamond"));
        var cobaltCost = ArcaneAnvilBlockEntity.cost(ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(cobalt, cobalt, cobalt)));
        var diamondCost = ArcaneAnvilBlockEntity.cost(ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(diamond, diamond, diamond)));
        if (cobaltCost.size() != 6 || cobaltCost.values().stream().anyMatch(value -> value != 1600))
            throw new AssertionError("Cobalt cost is not (4 + 4) x 2: " + cobaltCost);
        if (diamondCost.size() != 6 || diamondCost.values().stream().anyMatch(value -> value != 11200))
            throw new AssertionError("Diamond cost is not (14 + 14) x 4: " + diamondCost);
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void newWandCaps(GameTestHelper helper) {
        for (String name : new String[]{"tconstruct:cobalt", "tconstruct:cinderslime", "tconstruct:queens_slime",
                "tconstruct:hepatizon", "tconstruct:manyullyn", "tconstruct:blazing_bone", "tconstruct:knightslime",
                "tc_t:diamond", "tc_t:netherite"}) {
            var id = new MaterialId(name);
            if (!TCtItems.WAND_CAP.get().canUseMaterial(id)) throw new AssertionError("Missing cap material: " + name);
            var cap = TCtItems.WAND_CAP.get().withMaterial(id);
            if (cap.isEmpty()) throw new AssertionError("Cannot create cap: " + name);
            var materials = MaterialNBT.of(MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:wood")),
                    MaterialVariant.of((MaterialVariantId) id), MaterialVariant.of((MaterialVariantId) id));
            var wand = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), materials);
            if (WandToolStats.averageCost(ToolStack.from(wand).getStats()) >= 1.01f)
                throw new AssertionError("Incorrect vis cost for " + name);
        }
        for (String name : new String[]{"diamond", "netherite"}) {
            if (helper.getLevel().getRecipeManager().byKey(TCt.getResource("materials/" + name)).isEmpty())
                throw new AssertionError("Missing material recipe: " + name);
        }
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void materialAffinitiesAndMigration(GameTestHelper helper) {
        var iron = MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:iron"));
        var cobalt = MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:cobalt"));
        var fire = MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:blazing_bone"));
        ItemStack windWand = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(cobalt, iron, iron));
        ItemStack fireWand = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(fire, iron, iron));
        var windStats = WandItem.toolWithAspectStats(windWand).getStats();
        var fireStats = WandItem.toolWithAspectStats(fireWand).getStats();
        if (WandToolStats.capacity(windStats, Aspect.AER) != 85 || WandToolStats.capacity(windStats, Aspect.IGNIS) != 60)
            throw new AssertionError("Cobalt affinity was not applied");
        if (WandToolStats.capacity(fireStats, Aspect.IGNIS) != 80 || WandToolStats.capacity(fireStats, Aspect.AER) != 45)
            throw new AssertionError("Blazing bone affinity was not applied");
        var tool = ToolStack.from(windWand);
        tool.getPersistentData().putInt(WandItem.visKey(Aspect.AER), 1234);
        windWand.getOrCreateTag().putInt("tc_t_wand_stats_version", 1);
        windWand.getTagElement("tic_stats").putFloat("tc_t:vis_capacity_aer", 1f);
        var migrated = WandItem.toolWithAspectStats(windWand);
        if (WandToolStats.capacity(migrated.getStats(), Aspect.AER) != 85
                || migrated.getPersistentData().getInt(WandItem.visKey(Aspect.AER)) != 1234)
            throw new AssertionError("Existing wand did not migrate while preserving vis");
        ItemStack host = Tc4WandBridge.toHost(windWand);
        var runtime = MfclApi.runtime().orElseThrow();
        var old = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, host).orElseThrow();
        var tag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, old).tag());
        if (tag.getInt(Tc4WandBridge.CAPACITY_PREFIX + "aer") != 8500
                || tag.getInt(Tc4WandBridge.CAPACITY_PREFIX + "ignis") != 6000)
            throw new AssertionError("TC4 did not receive the per-aspect capacities");
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void netheriteWandStatsAndPayment(GameTestHelper helper) {
        var netherite = MaterialVariant.of((MaterialVariantId) new MaterialId("tc_t:netherite"));
        var silverwood = MaterialVariant.of((MaterialVariantId) new MaterialId("tc_t:silverwood"));
        ItemStack top = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(netherite, netherite, netherite));
        ItemStack silver = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(silverwood, netherite, netherite));
        var tool = WandItem.toolWithAspectStats(top);
        var silverStats = WandItem.toolWithAspectStats(silver).getStats();
        for (Aspect aspect : Aspect.values()) {
            if (WandToolStats.capacity(tool.getStats(), aspect) != 300 || WandToolStats.capacity(silverStats, aspect) != 200)
                throw new AssertionError("Incorrect netherite cap capacity for " + aspect);
            if (Math.abs(tool.getStats().get(WandToolStats.VIS_COST.get(aspect)) - .65f) > .0001f)
                throw new AssertionError("Incorrect netherite cap vis modifier for " + aspect);
            tool.getPersistentData().putInt(WandItem.visKey(aspect), 5000);
        }
        ItemStack host = Tc4WandBridge.toHost(top);
        var costs = new EnumMap<Aspect, Integer>(Aspect.class);
        for (Aspect aspect : Aspect.values()) costs.put(aspect, 1000);
        ItemStack paid = ArcaneVis.pay(FakePlayerFactory.getMinecraft(helper.getLevel()), host, costs, false).orElseThrow();
        var runtime = MfclApi.runtime().orElseThrow();
        var old = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, paid).orElseThrow();
        var tag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, old).tag());
        for (Aspect aspect : Aspect.values()) {
            int remaining = tag.getInt(aspect.getId());
            if (remaining != 4350) throw new AssertionError("TC4 did not apply 65% for " + aspect + ": " + remaining);
        }
        var copper = MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:copper"));
        ItemStack mixed = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(silverwood, copper, netherite));
        var mixedTool = WandItem.toolWithAspectStats(mixed);
        for (Aspect aspect : Aspect.values()) mixedTool.getPersistentData().putInt(WandItem.visKey(aspect), 5000);
        ItemStack mixedPaid = ArcaneVis.pay(FakePlayerFactory.getMinecraft(helper.getLevel()),
                Tc4WandBridge.toHost(mixed), costs, false).orElseThrow();
        var mixedOld = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, mixedPaid).orElseThrow();
        var mixedTag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, mixedOld).tag());
        for (Aspect aspect : Aspect.values()) {
            int expected = aspect == Aspect.ORDO || aspect == Aspect.PERDITIO ? 4175 : 4125;
            if (Math.abs(mixedTag.getInt(aspect.getId()) - expected) > 1)
                throw new AssertionError("Incorrect mixed-cap TC4 payment for " + aspect + ": " + mixedTag.getInt(aspect.getId()));
        }
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
        if (tile.calcResult(player).isEmpty()) throw new AssertionError("Preview unavailable without a wand");
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
    public static void newMaterialCraftingVis(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        helper.setBlock(new BlockPos(0, 1, 0), ArcaneAnvil.BLOCK.get());
        helper.setBlock(new BlockPos(1, 1, 0), TinkerTables.tinkersAnvil.get());
        var tile = (ArcaneAnvilBlockEntity) helper.getBlockEntity(new BlockPos(0, 1, 0));
        var ordinary = (TinkerStationBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 0));
        var netherite = new MaterialId("tc_t:netherite");
        var rod = TCtItems.WAND_ROD.get().withMaterial(netherite);
        var cap = TCtItems.WAND_CAP.get().withMaterial(netherite);
        for (var station : new TinkerStationBlockEntity[]{tile, ordinary}) {
            station.setItem(1, rod.copy()); station.setItem(2, cap.copy()); station.setItem(3, cap.copy());
        }
        var recipe = (ArcaneWandRecipe) level.getRecipeManager().byKey(TCt.getResource("tools/building/wand")).orElseThrow();
        if (recipe.matches(new TinkerStationContainerWrapper(ordinary), level)
                || !recipe.matches(new TinkerStationContainerWrapper(tile), level))
            throw new AssertionError("Netherite wand bypassed the arcane anvil");
        var result = tile.calcResult(player);
        var cost = ArcaneAnvilBlockEntity.cost(result);
        if (cost.size() != 6 || cost.values().stream().anyMatch(value -> value != 18000))
            throw new AssertionError("Incorrect netherite craft cost: " + cost);
        var menu = new ArcaneAnvilMenu(1, player.getInventory(), tile);
        var output = menu.slots.stream().filter(slot -> slot instanceof LazyResultSlot).findFirst().orElseThrow();
        if (result.isEmpty() || output.mayPickup(player)) throw new AssertionError("Preview or empty-wand restriction failed");
        var runtime = MfclApi.runtime().orElseThrow();
        runtime.accessor(Tc4WandBridge.WandRodFactory.class).create("tc_t_test_craft_250", 250,
                runtime.stack(new LegacyStack("minecraft:stick", 0, 1, "")), 1);
        var weak = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                "{rod:\"tc_t_test_craft_250\",cap:\"gold\",aer:17900,terra:17900,ignis:17900,aqua:17900,ordo:17900,perditio:17900}"));
        tile.wandSlot().setItem(0, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, weak, ItemStack.class).orElseThrow());
        if (output.mayPickup(player)) throw new AssertionError("Insufficient vis was accepted");
        var charged = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                "{rod:\"tc_t_test_craft_250\",cap:\"gold\",aer:19000,terra:19000,ignis:19000,aqua:19000,ordo:19000,perditio:19000}"));
        tile.wandSlot().setItem(0, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, charged, ItemStack.class).orElseThrow());
        if (!output.mayPickup(player)) throw new AssertionError("Sufficient vis was rejected");
        ItemStack taken = output.remove(1);
        if (taken.isEmpty()) throw new AssertionError("Preview did not produce output");
        output.onTake(player, taken);
        var legacy = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, tile.wandSlot().getItem(0)).orElseThrow();
        var tag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, legacy).tag());
        for (Aspect aspect : Aspect.values()) if (tag.getInt(aspect.getId()) != 1000)
            throw new AssertionError("Wrong crafting vis charge for " + aspect + ": " + tag.getInt(aspect.getId()));
        if (!tile.getItem(1).isEmpty() || !tile.getItem(2).isEmpty() || !tile.getItem(3).isEmpty())
            throw new AssertionError("Netherite craft failed to consume materials");
        for (String id : github.com.gengyoubo.TCt.stats.WandCapMaterials.CUSTOM)
            if (!ArcaneAnvilBlockEntity.advancedMaterial(id)) throw new AssertionError("Advanced material has no cost: " + id);
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID)
    public static void sceptreAndStaff(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var silverId = new MaterialId("tc_t:silverwood");
        var goldId = new MaterialId("tconstruct:gold");
        var silver = MaterialVariant.of((MaterialVariantId) silverId);
        var gold = MaterialVariant.of((MaterialVariantId) goldId);
        ItemStack sceptre = ToolBuildHandler.buildItemFromMaterials(TCtItems.SCEPTRE.get(), MaterialNBT.of(silver, gold, gold, gold));
        ItemStack staff = ToolBuildHandler.buildItemFromMaterials(TCtItems.STAFF.get(), MaterialNBT.of(silver, gold, gold));
        var sceptreTool = WandItem.toolWithAspectStats(sceptre);
        var staffTool = WandItem.toolWithAspectStats(staff);
        ItemStack oldStaffFormat = staff.copy();
        ToolStack.from(oldStaffFormat).getPersistentData().putInt(WandItem.visKey(Aspect.ORDO), 1234);
        oldStaffFormat.getOrCreateTag().put(ToolStack.TAG_MATERIALS, MaterialNBT.of(silver, silver, gold, gold).serializeToNBT());
        var migratedStaff = WandItem.toolWithAspectStats(oldStaffFormat);
        if (migratedStaff.getMaterials().size() != 3 || !migratedStaff.getMaterials().get(1).getVariant().getId().equals(goldId)
                || migratedStaff.getPersistentData().getInt(WandItem.visKey(Aspect.ORDO)) != 1234)
            throw new AssertionError("Old staff did not migrate its caps and stored vis");
        var layoutLoader = slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader.getInstance();
        var staffLayout = layoutLoader.get(TCt.getResource("staff"));
        var sceptreLayout = layoutLoader.get(TCt.getResource("sceptre"));
        if (staffLayout.getInputCount() != 3 || staffLayout.getSlot(1).getX() != 62 || staffLayout.getSlot(1).getY() != 77
                || sceptreLayout.getInputCount() != 5 || sceptreLayout.getSlot(2).getX() != 62 || sceptreLayout.getSlot(2).getY() != 53
                || sceptreLayout.getSlot(5).getX() != 86 || sceptreLayout.getSlot(5).getY() != 53)
            throw new AssertionError("Staff or sceptre layout does not match the intended grid");
        for (Aspect aspect : Aspect.values()) {
            if (WandToolStats.capacity(sceptreTool.getStats(), aspect) != 150
                    || WandToolStats.capacity(staffTool.getStats(), aspect) != 250)
                throw new AssertionError("Incorrect sceptre or staff capacity for " + aspect);
            sceptreTool.getPersistentData().putInt(WandItem.visKey(aspect), 2000);
        }
        if (!Tc4WandBridge.canCraftWith(sceptre) || Tc4WandBridge.canCraftWith(staff)
                || Tc4WandBridge.isNormalWand(sceptre) || Tc4WandBridge.isNormalWand(staff))
            throw new AssertionError("Staff/sceptre crafting roles are incorrect");
        var runtime = MfclApi.runtime().orElseThrow();
        var oldStaff = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, Tc4WandBridge.toHost(staff)).orElseThrow();
        var oldSceptre = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, Tc4WandBridge.toHost(sceptre)).orElseThrow();
        var staffItem = runtime.accessor(ArcaneVis.OldStack.class, oldStaff).item();
        var sceptreItem = runtime.accessor(ArcaneVis.OldStack.class, oldSceptre).item();
        if (!runtime.accessor(Tc4WandBridge.OldWand.class, staffItem).isStaff(oldStaff)
                || !runtime.accessor(Tc4WandBridge.OldWand.class, sceptreItem).isSceptre(oldSceptre))
            throw new AssertionError("TC4 did not recognize staff/sceptre");
        if (runtime.accessor(Tc4WandBridge.OldWand.class, staffItem).getMaxVis(oldStaff) != 25000
                || runtime.accessor(Tc4WandBridge.OldWand.class, sceptreItem).getMaxVis(oldSceptre) != 15000)
            throw new AssertionError("TC4 staff/sceptre maximum capacity is incorrect");
        var costs = new EnumMap<Aspect, Integer>(Aspect.class);
        for (Aspect aspect : Aspect.values()) costs.put(aspect, 1000);
        ItemStack paid = ArcaneVis.pay(player, sceptre, costs, false).orElseThrow();
        var paidTool = WandItem.toolWithAspectStats(paid);
        for (Aspect aspect : Aspect.values()) if (paidTool.getPersistentData().getInt(WandItem.visKey(aspect)) != 1100)
            throw new AssertionError("Sceptre crafting discount failed for " + aspect);
        if (staff.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).isEmpty())
            throw new AssertionError("Staff has no melee attack attribute");

        helper.setBlock(new BlockPos(0, 1, 0), ArcaneAnvil.BLOCK.get());
        helper.setBlock(new BlockPos(1, 1, 0), TinkerTables.tinkersAnvil.get());
        var tile = (ArcaneAnvilBlockEntity) helper.getBlockEntity(new BlockPos(0, 1, 0));
        var ordinary = (TinkerStationBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 0));
        var sceptreRecipe = (ArcaneWandRecipe) level.getRecipeManager().byKey(TCt.getResource("tools/building/sceptre")).orElseThrow();
        var staffRecipe = (ArcaneWandRecipe) level.getRecipeManager().byKey(TCt.getResource("tools/building/staff")).orElseThrow();
        ItemStack rod = TCtItems.WAND_ROD.get().withMaterial(silverId);
        ItemStack cap = TCtItems.WAND_CAP.get().withMaterial(goldId);
        var charm = runtime.stack(new LegacyStack("Thaumcraft:ItemResource", 15, 1, ""));
        ItemStack hostCharm = runtime.mappings().toHost(LegacyMappings.ITEM_STACK, charm, ItemStack.class).orElseThrow();
        for (var station : new TinkerStationBlockEntity[]{tile, ordinary}) {
            station.setItem(1, rod.copy()); station.setItem(2, cap.copy()); station.setItem(3, cap.copy());
            station.setItem(4, cap.copy()); station.setItem(5, hostCharm.copy());
        }
        if (!sceptreRecipe.matches(new TinkerStationContainerWrapper(tile), level)
                || sceptreRecipe.matches(new TinkerStationContainerWrapper(ordinary), level))
            throw new AssertionError("Sceptre recipe, charm, or anvil restriction failed");
        ItemStack preview = tile.calcResult(player);
        if (!preview.is(TCtItems.SCEPTRE.get()) || ArcaneAnvilBlockEntity.cost(preview).values().stream().anyMatch(n -> n != 4050))
            throw new AssertionError("Incorrect sceptre output or crafting cost");
        var charge = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                "{rod:\"silverwood\",cap:\"gold\",aer:10000,terra:10000,ignis:10000,aqua:10000,ordo:10000,perditio:10000}"));
        tile.wandSlot().setItem(0, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, charge, ItemStack.class).orElseThrow());
        var menu = new ArcaneAnvilMenu(1, player.getInventory(), tile);
        var output = menu.slots.stream().filter(slot -> slot instanceof LazyResultSlot).findFirst().orElseThrow();
        if (!output.mayPickup(player)) throw new AssertionError("Charged sceptre recipe cannot be crafted");
        ItemStack taken = output.remove(1);
        if (!taken.is(TCtItems.SCEPTRE.get())) throw new AssertionError("Sceptre recipe output is wrong");
        output.onTake(player, taken);
        for (int i = 1; i <= 5; i++) if (!tile.getItem(i).isEmpty())
            throw new AssertionError("Sceptre crafting did not consume input " + i);
        var afterCraft = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, tile.wandSlot().getItem(0)).orElseThrow();
        var afterTag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, afterCraft).tag());
        for (Aspect aspect : Aspect.values()) if (afterTag.getInt(aspect.getId()) != 5950)
            throw new AssertionError("Sceptre crafting did not consume vis for " + aspect);
        tile.setItem(1, rod.copy()); tile.setItem(2, cap.copy()); tile.setItem(3, cap.copy());
        tile.setItem(4, TCtItems.WAND_CAP.get().withMaterial(new MaterialId("tconstruct:iron"))); tile.setItem(5, hostCharm.copy());
        if (sceptreRecipe.matches(new TinkerStationContainerWrapper(tile), level))
            throw new AssertionError("Sceptre recipe accepted different caps");
        ItemStack staffRod = TCtItems.STAFF_ROD.get().withMaterial(silverId);
        tile.setItem(1, TCtItems.STAFF_ROD_BLANK.get().withMaterial(silverId)); tile.setItem(2, cap.copy()); tile.setItem(3, cap.copy()); tile.setItem(4, ItemStack.EMPTY); tile.setItem(5, ItemStack.EMPTY);
        if (staffRecipe.matches(new TinkerStationContainerWrapper(tile), level))
            throw new AssertionError("Unfinished staff core was accepted as a finished part");
        tile.setItem(1, staffRod);
        if (!staffRecipe.matches(new TinkerStationContainerWrapper(tile), level))
            throw new AssertionError("Staff recipe rejected its parts");
        ItemStack staffPreview = tile.calcResult(player);
        if (!staffPreview.is(TCtItems.STAFF.get()) || ArcaneAnvilBlockEntity.cost(staffPreview).values().stream().anyMatch(n -> n != 2700))
            throw new AssertionError("Incorrect staff output or crafting cost");
        if (!output.mayPickup(player)) throw new AssertionError("Charged staff recipe cannot be crafted");
        ItemStack staffTaken = output.remove(1);
        if (!staffTaken.is(TCtItems.STAFF.get())) throw new AssertionError("Staff recipe output is wrong");
        output.onTake(player, staffTaken);
        for (int i = 1; i <= 3; i++) if (!tile.getItem(i).isEmpty())
            throw new AssertionError("Staff crafting did not consume input " + i);
        var afterStaff = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, tile.wandSlot().getItem(0)).orElseThrow();
        var staffTag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, afterStaff).tag());
        for (Aspect aspect : Aspect.values()) if (staffTag.getInt(aspect.getId()) != 3250)
            throw new AssertionError("Staff crafting did not consume vis for " + aspect);
        if (tile.wandSlot().canPlaceItem(0, staff) || !tile.wandSlot().canPlaceItem(0, sceptre))
            throw new AssertionError("Workbench wand slot accepted staff or rejected sceptre");
        helper.succeed();
    }

    @GameTest(template = "arcane_test", templateNamespace = TCt.MOD_ID, timeoutTicks = 400)
    public static void staffCoreCasting(GameTestHelper helper) {
        var level = helper.getLevel();
        var local = new BlockPos(0, 1, 0);
        helper.setBlock(local, slimeknights.tconstruct.smeltery.TinkerSmeltery.searedTable.get());
        var table = (slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity.Table) helper.getBlockEntity(local);
        table.setItem(slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity.INPUT, new ItemStack(TCtItems.STAFF_ROD_CAST.get()));
        var fluid = new net.minecraftforge.fluids.FluidStack(slimeknights.tconstruct.fluids.TinkerFluids.moltenIron.get(), 360);
        if (level.getRecipeManager().byKey(TCt.getResource("tools/parts/builder/staff_rod_blank")).isEmpty())
            throw new AssertionError("Solid-material unfinished staff core recipe is missing");
        if (StaffRodCasting.orderCost(TCtItems.STAFF_ROD_BLANK.get().withMaterial(new MaterialId("tc_t:silverwood"))) != 3000
                || StaffRodCasting.orderCost(TCtItems.STAFF_ROD_BLANK.get().withMaterial(new MaterialId("tc_t:netherite"))) != 4000)
            throw new AssertionError("Staff core vis cost does not follow material tiers");
        int filled = table.getTank().fill(fluid, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (filled != 360) throw new AssertionError("Staff-core cast did not accept four ingots of iron: " + filled);
        int delay = Math.max(1, table.getCoolingTime()) + 5;
        helper.runAfterDelay(delay, () -> {
            int slot = table.getItem(1).is(TCtItems.STAFF_ROD_BLANK.get()) ? 1 : table.getItem(0).is(TCtItems.STAFF_ROD_BLANK.get()) ? 0 : -1;
            if (slot < 0) throw new AssertionError("Casting did not produce an unfinished staff core");
            ItemStack blank = table.getItem(slot);
            if (!new MaterialId("tconstruct:iron").equals(slimeknights.tconstruct.library.tools.part.IMaterialItem.getMaterialFromStack(blank).getId()))
                throw new AssertionError("Casting lost its material");
            if (StaffRodCasting.orderCost(blank) != 2000) throw new AssertionError("Iron tier order cost is not 20 vis");
            var player = FakePlayerFactory.getMinecraft(level);
            var runtime = MfclApi.runtime().orElseThrow();
            var charge = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                    "{rod:\"silverwood\",cap:\"gold\",aer:3000,terra:3000,ignis:3000,aqua:3000,ordo:3000,perditio:3000}"));
            ItemStack charged = runtime.mappings().toHost(LegacyMappings.ITEM_STACK, charge, ItemStack.class).orElseThrow();
            var pos = helper.absolutePos(local);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, charged.copy());
            var early = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(early);
            if (!early.isCanceled() || !table.getItem(slot).is(TCtItems.STAFF_ROD_BLANK.get()) || !ItemStack.matches(charged, player.getMainHandItem()))
                throw new AssertionError("Wand was accepted before the charm");
            var charm = runtime.stack(new LegacyStack("Thaumcraft:ItemResource", 15, 1, ""));
            player.setItemInHand(InteractionHand.MAIN_HAND, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, charm, ItemStack.class).orElseThrow());
            var first = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(first);
            if (!first.isCanceled() || !player.getMainHandItem().isEmpty() || !table.getItem(slot).getOrCreateTag().getBoolean(StaffRodCasting.ATTUNED))
                throw new AssertionError("Charm was not consumed or the core was not attuned");
            player.setItemInHand(InteractionHand.MAIN_HAND, runtime.mappings().toHost(LegacyMappings.ITEM_STACK, charm, ItemStack.class).orElseThrow());
            var repeat = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(repeat);
            if (!repeat.isCanceled() || player.getMainHandItem().isEmpty())
                throw new AssertionError("A second charm was consumed");
            var restored = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(table.getBlockPos(), table.getBlockState(), table.saveWithFullMetadata());
            if (!(restored instanceof slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity.Table copy)
                    || !copy.getItem(slot).getOrCreateTag().getBoolean(StaffRodCasting.ATTUNED))
                throw new AssertionError("Attunement did not survive save/load");
            var low = runtime.stack(new LegacyStack("Thaumcraft:WandCasting", 0, 1,
                    "{rod:\"silverwood\",cap:\"gold\",aer:3000,terra:3000,ignis:3000,aqua:3000,ordo:1900,perditio:3000}"));
            ItemStack weak = runtime.mappings().toHost(LegacyMappings.ITEM_STACK, low, ItemStack.class).orElseThrow();
            player.setItemInHand(InteractionHand.MAIN_HAND, weak.copy());
            var insufficient = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(insufficient);
            if (!insufficient.isCanceled() || !table.getItem(slot).is(TCtItems.STAFF_ROD_BLANK.get()) || !ItemStack.matches(weak, player.getMainHandItem()))
                throw new AssertionError("Insufficient order vis was accepted or spent");
            player.setItemInHand(InteractionHand.MAIN_HAND, charged.copy());
            var finish = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(finish);
            if (!finish.isCanceled() || !table.getItem(slot).is(TCtItems.STAFF_ROD.get())
                    || !new MaterialId("tconstruct:iron").equals(slimeknights.tconstruct.library.tools.part.IMaterialItem.getMaterialFromStack(table.getItem(slot)).getId()))
                throw new AssertionError("The final iron staff core was not produced");
            var old = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, player.getMainHandItem()).orElseThrow();
            var tag = runtime.accessor(Tc4WandBridge.OldTag.class, runtime.accessor(Tc4WandBridge.OldStack.class, old).tag());
            if (tag.getInt("ordo") != 1000 || tag.getInt("aer") != 3000)
                throw new AssertionError("Wrong vis spent during staff core awakening");
            if (!table.getItem(1 - slot).is(TCtItems.STAFF_ROD_CAST.get())) throw new AssertionError("Reusable cast was consumed");
            ItemStack another = TCtItems.STAFF_ROD_BLANK.get().withMaterial(new MaterialId("tconstruct:iron"));
            another.getOrCreateTag().putBoolean(StaffRodCasting.ATTUNED, true);
            table.setItem(slot, another);
            var silver = MaterialVariant.of((MaterialVariantId) new MaterialId("tc_t:silverwood"));
            var gold = MaterialVariant.of((MaterialVariantId) new MaterialId("tconstruct:gold"));
            ItemStack tinkersWand = ToolBuildHandler.buildItemFromMaterials(TCtItems.WAND.get(), MaterialNBT.of(silver, gold, gold));
            var tinkersTool = WandItem.toolWithAspectStats(tinkersWand);
            tinkersTool.getPersistentData().putInt(WandItem.visKey(Aspect.ORDO), 3000);
            tinkersTool.getPersistentData().putInt(WandItem.visKey(Aspect.AER), 3000);
            player.setItemInHand(InteractionHand.MAIN_HAND, tinkersWand);
            var tinkersFinish = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            MinecraftForge.EVENT_BUS.post(tinkersFinish);
            if (!tinkersFinish.isCanceled() || !table.getItem(slot).is(TCtItems.STAFF_ROD.get())
                    || !player.getMainHandItem().is(TCtItems.WAND.get()))
                throw new AssertionError("Tinkers wand could not finish the staff core");
            var afterTool = WandItem.toolWithAspectStats(player.getMainHandItem());
            if (afterTool.getPersistentData().getInt(WandItem.visKey(Aspect.ORDO)) != 1000
                    || afterTool.getPersistentData().getInt(WandItem.visKey(Aspect.AER)) != 3000)
                throw new AssertionError("Tinkers wand vis was not synchronized after awakening");
            helper.succeed();
        });
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
