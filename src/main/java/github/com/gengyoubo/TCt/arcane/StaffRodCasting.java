package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.compat.tc4.ArcaneVis;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import github.com.gengyoubo.TCt.item.TCtItems;
import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.mfcl.api.LegacyMappings;
import github.com.gengyoubo.mfcl.api.LegacyStack;
import github.com.gengyoubo.mfcl.api.MfclApi;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Awakens a cooled staff-core casting on a Tinkers casting table in two ordered steps. */
@Mod.EventBusSubscriber(modid = TCt.MOD_ID)
public final class StaffRodCasting {
    private StaffRodCasting() { }
    public static final String ATTUNED = "tc_t_staff_core_attuned";
    public static final TagKey<Item> CHARM = TagKey.create(Registries.ITEM, TCt.getResource("primal_charm"));

    private static final ResourceLocation TC4_CHARM = ResourceLocation.fromNamespaceAndPath("thaumcraft", "item_resource/meta_15");

    /** Tag ingredients normally discard the legacy metadata stored on MFCL item stacks. */
    public static List<ItemStack> displayCharms() {
        List<ItemStack> result = new ArrayList<>();
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(CHARM)) {
            Item item = holder.value();
            if (TC4_CHARM.equals(BuiltInRegistries.ITEM.getKey(item))) {
                MfclApi.runtime().flatMap(runtime -> runtime.mappings().toHost(LegacyMappings.ITEM_STACK,
                        runtime.stack(new LegacyStack("Thaumcraft:ItemResource", 15, 1, "")), ItemStack.class))
                        .filter(stack -> !stack.isEmpty() && stack.is(CHARM)).ifPresent(result::add);
            } else {
                result.add(item.getDefaultInstance());
            }
        }
        return result;
    }

    public static boolean isCharm(ItemStack stack) {
        if (!stack.is(CHARM)) return false;
        if (!TC4_CHARM.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) return true;
        return MfclApi.runtime().flatMap(runtime -> runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, stack)
                .map(runtime::snapshot)).map(legacy -> legacy.metadata() == 15).orElse(false);
    }

    public static int orderCost(ItemStack blank) {
        var material = IMaterialItem.getMaterialFromStack(blank);
        if (material == null) return 0;
        var definition = MaterialRegistry.getInstance().getMaterial(material.getId());
        if (definition == null || definition.getIdentifier().equals(slimeknights.tconstruct.library.materials.definition.MaterialId.UNKNOWN)) return 0;
        return Math.max(1, definition.getTier()) * ArcaneAnvilConfig.STAFF_ROD_ORDER_PER_TIER.get() * 100;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CastingBlockEntity.Table table)
                || !table.getTank().isEmpty() || table.getTimer() > 0) return;
        int slot = table.getItem(CastingBlockEntity.OUTPUT).is(TCtItems.STAFF_ROD_BLANK.get()) ? CastingBlockEntity.OUTPUT
                : table.getItem(CastingBlockEntity.INPUT).is(TCtItems.STAFF_ROD_BLANK.get()) ? CastingBlockEntity.INPUT : -1;
        if (slot < 0) return;
        ItemStack held = event.getItemStack();
        boolean charm = isCharm(held);
        boolean wand = !charm && Tc4WandBridge.isHostWand(held);
        if (!charm && !wand) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        if (event.getLevel().isClientSide) return;
        ItemStack blank = table.getItem(slot);
        boolean attuned = blank.getOrCreateTag().getBoolean(ATTUNED);
        if (charm) {
            if (attuned) {
                event.getEntity().displayClientMessage(Component.translatable("message.tc_t.staff_rod.already_attuned"), true);
                return;
            }
            ItemStack updated = blank.copy();
            updated.getOrCreateTag().putBoolean(ATTUNED, true);
            table.setItem(slot, updated);
            if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
            int amount = orderCost(updated) / 100;
            event.getEntity().displayClientMessage(Component.translatable("message.tc_t.staff_rod.attuned", amount), true);
        } else {
            if (!attuned) {
                event.getEntity().displayClientMessage(Component.translatable("message.tc_t.staff_rod.need_charm"), true);
                return;
            }
            if (!Tc4WandBridge.isNormalWand(held) || !(event.getEntity() instanceof ServerPlayer player)) {
                event.getEntity().displayClientMessage(Component.translatable("message.tc_t.staff_rod.need_wand"), true);
                return;
            }
            int cost = orderCost(blank);
            var material = IMaterialItem.getMaterialFromStack(blank);
            if (cost <= 0 || material == null) return;
            ItemStack result = TCtItems.STAFF_ROD.get().withMaterial(material);
            if (result.isEmpty()) return;
            var paid = ArcaneVis.pay(player, held, Map.of(Aspect.ORDO, cost), false);
            if (paid.isEmpty()) {
                event.getEntity().displayClientMessage(Component.translatable("message.tc_t.staff_rod.insufficient", cost / 100), true);
                return;
            }
            table.setItem(slot, result);
            player.setItemInHand(event.getHand(), paid.get());
            event.getEntity().displayClientMessage(Component.translatable("message.tc_t.staff_rod.finished"), true);
        }
        table.setChanged();
        var state = table.getBlockState();
        event.getLevel().sendBlockUpdated(event.getPos(), state, state, 3);
        event.getLevel().playSound(null, event.getPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.7f, 1.2f);
    }
}
