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
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getCount() == 1 && github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge.isHostWand(stack); }
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
    }

    public static boolean advancedMaterial(String id) {
        return id.equals("tc_t:silverwood") || id.equals("tconstruct:gold");
    }

    /** TC4's original recipe multiplies rod and cap craft costs for each primal aspect. */
    public static Map<Aspect, Integer> cost(ItemStack output) {
        if (!(output.getItem() instanceof WandItem)) return Map.of();
        var materials = ToolStack.from(output).getMaterials();
        if (materials.size() < 3) return Map.of();
        String rod = materials.get(0).getVariant().getId().toString();
        String cap1 = materials.get(1).getVariant().getId().toString();
        String cap2 = materials.get(2).getVariant().getId().toString();
        if (!advancedMaterial(rod) && !advancedMaterial(cap1) && !advancedMaterial(cap2)) return Map.of();
        int rodCost = rod.equals("tc_t:silverwood") ? ArcaneAnvilConfig.SILVERWOOD_ROD.get() : 1;
        int capCost = Math.max(capCost(cap1), capCost(cap2));
        var cost = new EnumMap<Aspect, Integer>(Aspect.class);
        for (var aspect : Aspect.values()) cost.put(aspect, rodCost * capCost * 100);
        return cost;
    }
    private static int capCost(String id) {
        return id.equals("tconstruct:gold") ? ArcaneAnvilConfig.GOLD_CAP.get() : id.equals("tconstruct:copper") ? ArcaneAnvilConfig.COPPER_CAP.get() : 1;
    }

    public boolean canPay(Player player, ItemStack result) {
        if (!(result.getItem() instanceof WandItem)) return false;
        var cost = cost(result);
        return cost.isEmpty() || player instanceof ServerPlayer server && ArcaneVis.pay(server, wand.getItem(0), cost, true).isPresent();
    }

    @Override public ItemStack calcResult(Player player) {
        ItemStack result = super.calcResult(player);
        return !result.isEmpty() && (!(result.getItem() instanceof WandItem)
                || player instanceof ServerPlayer && !canPay(player, result)) ? ItemStack.EMPTY : result;
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
