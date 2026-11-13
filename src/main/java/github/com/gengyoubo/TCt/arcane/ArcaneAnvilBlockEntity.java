package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.compat.tc4.ArcaneVis;
import github.com.gengyoubo.TCt.item.WandItem;
import github.com.gengyoubo.TCt.stats.Aspect;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

import java.util.EnumMap;
import java.util.Map;

public final class ArcaneAnvilBlockEntity extends TinkerStationBlockEntity {
    private final SimpleContainer wand = new SimpleContainer(1) {
        @Override public void setChanged() { super.setChanged(); ArcaneAnvilBlockEntity.this.setChanged(); getCraftingResult().clearContent(); }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getCount() == 1 && github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge.canCraftWith(stack); }
        @Override public int getMaxStackSize() { return 1; }
    };
    public ArcaneAnvilBlockEntity(BlockPos pos, BlockState state) { super(pos, state, 6); }
    @Override public BlockEntityType<?> getType() { return ArcaneAnvil.TILE.get(); }
    public SimpleContainer wandSlot() { return wand; }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ArcaneAnvilMenu(id, inventory, this); }

    @Override public void saveSynced(CompoundTag tag) {
        super.saveSynced(tag);
        if (!wand.getItem(0).isEmpty()) tag.put("ArcaneWand", wand.getItem(0).save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        wand.setItem(0, tag.contains("ArcaneWand", 10) ? ItemStack.of(tag.getCompound("ArcaneWand")) : ItemStack.EMPTY);
        if (level != null && level.isClientSide) {
            // The texture arrives after the replacement block; rebuild its cached model data.
            requestModelDataUpdate();
            var state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 0);
        }
    }

    // Base cost per primal aspect, before the crafting wand and equipment modifiers.
    private static final Map<String, Integer> ROD_COSTS = Map.ofEntries(
            Map.entry("tconstruct:cobalt", 4), Map.entry("tconstruct:steel", 4),
            Map.entry("tconstruct:blazing_bone", 4), Map.entry("tconstruct:nicrosil", 5),
            Map.entry("tconstruct:knightmetal", 5), Map.entry("tconstruct:cinderslime", 6),
            Map.entry("tconstruct:queens_slime", 7), Map.entry("tconstruct:hepatizon", 8),
            Map.entry("tconstruct:knightslime", 8), Map.entry("tconstruct:manyullyn", 10),
            Map.entry("tc_t:diamond", 14), Map.entry("tc_t:netherite", 18));
    private static final Map<String, Integer> CAP_COSTS = Map.ofEntries(
            Map.entry("tconstruct:cobalt", 2), Map.entry("tconstruct:steel", 2),
            Map.entry("tconstruct:blazing_bone", 2), Map.entry("tconstruct:nicrosil", 2),
            Map.entry("tconstruct:knightmetal", 3), Map.entry("tconstruct:cinderslime", 2),
            Map.entry("tconstruct:queens_slime", 3), Map.entry("tconstruct:hepatizon", 3),
            Map.entry("tconstruct:knightslime", 3), Map.entry("tconstruct:manyullyn", 4),
            Map.entry("tc_t:diamond", 4), Map.entry("tc_t:netherite", 5));

    public static boolean advancedMaterial(String id) {
        return id.equals("tc_t:silverwood") || id.equals("tconstruct:gold")
                || ROD_COSTS.containsKey(id) || CAP_COSTS.containsKey(id);
    }

    /** Each primal aspect costs two rod values times the more expensive cap value. */
    public static Map<Aspect, Integer> cost(ItemStack output) {
        if (!(output.getItem() instanceof WandItem)) return Map.of();
        var materials = WandItem.toolWithAspectStats(output).getMaterials();
        if (materials.size() < 3) return Map.of();
        WandItem.Kind kind = ((WandItem) output.getItem()).kind();
        int rodCount = 1;
        int capCount = kind == WandItem.Kind.SCEPTRE ? 3 : 2;
        if (materials.size() < rodCount + capCount) return Map.of();
        double rods = 0;
        int caps = 0;
        boolean advanced = kind != WandItem.Kind.WAND;
        for (int i = 0; i < rodCount; i++) {
            String id = materials.get(i).getVariant().getId().toString();
            advanced |= advancedMaterial(id);
            rods += id.equals("tc_t:silverwood") ? ArcaneAnvilConfig.SILVERWOOD_ROD.get() : ROD_COSTS.getOrDefault(id, 1);
        }
        if (rodCount == 1) rods *= 2;
        for (int i = rodCount; i < rodCount + capCount; i++) {
            String id = materials.get(i).getVariant().getId().toString();
            advanced |= advancedMaterial(id);
            caps = Math.max(caps, capCost(id));
        }
        if (!advanced) return Map.of();
        var cost = new EnumMap<Aspect, Integer>(Aspect.class);
        int centivis = (int) Math.round(rods * caps * (kind == WandItem.Kind.SCEPTRE ? 1.5 : 1.0) * 100);
        for (var aspect : Aspect.values()) cost.put(aspect, centivis);
        return cost;
    }
    private static int capCost(String id) {
        return id.equals("tconstruct:gold") ? ArcaneAnvilConfig.GOLD_CAP.get()
                : id.equals("tconstruct:copper") ? ArcaneAnvilConfig.COPPER_CAP.get() : CAP_COSTS.getOrDefault(id, 1);
    }

    public boolean canPay(Player player, ItemStack result) {
        if (!(result.getItem() instanceof WandItem)) return false;
        var cost = cost(result);
        return cost.isEmpty() || player instanceof ServerPlayer server && ArcaneVis.pay(server, wand.getItem(0), cost, true).isPresent();
    }

    @Override public ItemStack calcResult(Player player) {
        ItemStack result = super.calcResult(player);
        // Keep a visible preview so the player can see the required aspects before inserting a wand.
        // The result slot checks canPay on the server before allowing pickup.
        return !result.isEmpty() && !(result.getItem() instanceof WandItem) ? ItemStack.EMPTY : result;
    }

    @Override public void onCraft(Player player, ItemStack result, int count) {
        if (count <= 0 || !(result.getItem() instanceof WandItem)) return;
        var cost = cost(result);
        if (!cost.isEmpty()) {
            if (!(player instanceof ServerPlayer server)) return;
            var paid = ArcaneVis.pay(server, wand.getItem(0), cost, false);
            if (paid.isEmpty()) return;
            wand.setItem(0, paid.get());
        }
        super.onCraft(player, result, count);
    }
}
