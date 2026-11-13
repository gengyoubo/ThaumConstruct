package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.TCt;
import net.minecraft.network.FriendlyByteBuf;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.menu.slot.ArmorSlot;
import slimeknights.tconstruct.tables.menu.slot.LazyResultSlot;

import static github.com.gengyoubo.TCt.arcane.ArcaneAnvilLayout.*;

public final class ArcaneAnvilMenu extends TinkerStationContainerMenu {
    private final ArcaneAnvilBlockEntity anvil;
    private int outputIndex = -1;
    private int wandIndex = -1;
    private boolean wandSelected;
    private Inventory playerInventory;
    private boolean wandView = true;

    public ArcaneAnvilMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (ArcaneAnvilBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }
    public ArcaneAnvilMenu(int id, Inventory inventory, ArcaneAnvilBlockEntity anvil) {
        super(id, inventory, anvil);
        this.anvil = anvil;
        this.playerInventory = inventory;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) instanceof LazyResultSlot old) {
                outputIndex = i;
                var replacement = new LazyResultSlot(anvil.getCraftingResult(), RESULT_X, RESULT_Y) {
                    @Override public boolean mayPickup(Player player) {
                        ItemStack result = getItem();
                        return super.mayPickup(player) && (player.level().isClientSide || anvil.canPay(player, result));
                    }
                };
                replacement.index = i;
                slots.set(i, replacement);
                break;
            }
        }
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).container == anvil.wandSlot()) { wandIndex = i; break; }
        if (wandIndex < 0) throw new IllegalStateException("Arcane anvil wand slot was not created");
        // the TC4 cloth takes the upper left, so the armor goes next to the inventory; the offhand slot is not shown
        int armor = 0;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot instanceof ArmorSlot && armor < ARMOR_Y.length) {
                slot.x = ARMOR_X;
                slot.y = ARMOR_Y[armor++];
            } else if (slot.container == inventory && slot.getContainerSlot() == Inventory.SLOT_OFFHAND) {
                // replaced in place so the indices of the following slots stay the same
                Slot hidden = new Slot(inventory, Inventory.SLOT_OFFHAND, slot.x, slot.y) {
                    @Override public boolean isActive() { return !wandSelected; }
                };
                hidden.setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
                hidden.index = i;
                slots.set(i, hidden);
            }
        }
    }
    @Override protected void addInventorySlots() {
        if (getTile() instanceof ArcaneAnvilBlockEntity anvil) {
            // right of the cloth; ArcaneAnvilScreen draws the TC4 wand box around it
            addSlot(new Slot(anvil.wandSlot(), 0, WAND_X, WAND_Y) {
                @Override public boolean isActive() { return wandSelected; }
                @Override public boolean mayPlace(ItemStack stack) { return wandSelected && container.canPlaceItem(0, stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        super.addInventorySlots();
    }
    @Override public void setToolSelection(StationSlotLayout layout) {
        var id = layout.getName();
        if (!id.equals(TCt.getResource("wand")) && !id.equals(TCt.getResource("sceptre")) && !id.equals(TCt.getResource("staff")))
            layout = StationSlotLayoutLoader.getInstance().get(TCt.getResource("wand"));
        super.setToolSelection(layout);
        wandSelected = true;
    }

    /** Move the shared menu slots to match the currently displayed panel. */
    public void setWandView(boolean wand) {
        wandSelected = wand;
        if (wandView != wand) {
            for (Slot slot : slots) {
                if (slot.container == playerInventory && slot.getContainerSlot() < 36) {
                    slot.y += wand ? INVENTORY_SHIFT : -INVENTORY_SHIFT;
                }
            }
            wandView = wand;
        }
        Slot result = slots.get(outputIndex);
        result.x = wand ? RESULT_X : 114;
        result.y = wand ? RESULT_Y : 38;
        int armor = 0;
        for (Slot slot : slots) {
            if (slot instanceof ArmorSlot && armor < ARMOR_Y.length) {
                slot.x = wand ? ARMOR_X : 152;
                slot.y = wand ? ARMOR_Y[armor] : 20 + 18 * armor;
                armor++;
            } else if (slot.container == playerInventory && slot.getContainerSlot() == Inventory.SLOT_OFFHAND) {
                slot.x = 132;
                slot.y = 74;
            }
        }
    }
    @Override protected int getInventoryYOffset() { return super.getInventoryYOffset() + INVENTORY_SHIFT; }
    @Override public MenuType<?> getType() { return ArcaneAnvil.MENU.get(); }
    public Slot wandSlot() { return slots.get(wandIndex); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        // Shift-crafting can perform several crafts at once. Require a normal click until each charge is atomic.
        if (index == outputIndex) return ItemStack.EMPTY;
        if (index == wandIndex) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack original = slot.getItem().copy();
            if (!moveItemStackTo(slot.getItem(), playerInventoryStart, playerInventoryStart + 36, true)) return ItemStack.EMPTY;
            if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            return original;
        }
        return super.quickMoveStack(player, index);
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.index != outputIndex && super.canTakeItemForPickAll(stack, slot);
    }
}
