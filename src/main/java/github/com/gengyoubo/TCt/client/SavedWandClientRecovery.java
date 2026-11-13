package github.com.gengyoubo.TCt.client;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.compat.tc4.SavedWandRecovery;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/** Remote clients do not read server chunk NBT, so restore definitions from synced tile data as it arrives. */
@Mod.EventBusSubscriber(modid = TCt.MOD_ID, value = Dist.CLIENT)
public final class SavedWandClientRecovery {
    private SavedWandClientRecovery() { }
    private static final Map<BlockEntity, Integer> SEEN = new WeakHashMap<>();

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Tc4WandBridge.available() || mc.level.getGameTime() % 20 != 0) return;
        int centerX = mc.player.chunkPosition().x, centerZ = mc.player.chunkPosition().z;
        int radius = mc.options.getEffectiveRenderDistance();
        for (int x = centerX - radius; x <= centerX + radius; x++) for (int z = centerZ - radius; z <= centerZ + radius; z++) {
            var chunk = mc.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (chunk == null) continue;
            for (var tile : chunk.getBlockEntities().values()) {
                if (!BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()).getNamespace().equals("thaumcraft")) continue;
                var saved = tile.getUpdateTag();
                if (!saved.contains("mfcl_legacy_nbt", Tag.TAG_BYTE_ARRAY)) continue;
                int hash = Arrays.hashCode(saved.getByteArray("mfcl_legacy_nbt"));
                if (Integer.valueOf(hash).equals(SEEN.put(tile, hash))) continue;
                SavedWandRecovery.restoreTile(saved);
            }
        }
    }
}
