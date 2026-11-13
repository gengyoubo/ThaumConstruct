package github.com.gengyoubo.TCt.arcane;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;

@Mod.EventBusSubscriber(modid = TCt.MOD_ID)
public final class ArcaneAnvilConversion {
    private ArcaneAnvilConversion() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var pos = event.getPos();
        BlockState original = level.getBlockState(pos);
        if (!event.getEntity().isShiftKeyDown() || !original.is(TinkerTables.tinkersAnvil.get()) || !Tc4WandBridge.available()
                || !Tc4WandBridge.isHostWand(event.getItemStack())) return;
        if (!(level.getBlockEntity(pos) instanceof TinkerStationBlockEntity old)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        var data = old.saveWithoutMetadata();
        BlockState replacement = ArcaneAnvil.BLOCK.get().defaultBlockState();
        for (Property<?> property : original.getProperties()) replacement = copy(original, replacement, property);
        old.clearContent();
        if (!level.setBlock(pos, replacement, 3) || !(level.getBlockEntity(pos) instanceof ArcaneAnvilBlockEntity next)) {
            level.setBlock(pos, original, 3);
            var restored = level.getBlockEntity(pos);
            if (restored != null) { restored.load(data); restored.setChanged(); }
            return;
        }
        next.load(data);
        next.setChanged();
        level.sendBlockUpdated(pos, replacement, replacement, 3);
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1, 1);
        event.getEntity().displayClientMessage(Component.translatable("message.tc_t.arcane_anvil.converted"), true);
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }
}
