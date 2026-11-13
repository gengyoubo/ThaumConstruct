package github.com.gengyoubo.TCt.compat.tc4;

import com.mojang.logging.LogUtils;
import github.com.gengyoubo.TCt.TCt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** Restore transient TC4 wand definitions before MFCL materializes saved legacy inventories. */
@Mod.EventBusSubscriber(modid = TCt.MOD_ID)
public final class SavedWandRecovery {
    private SavedWandRecovery() { }

    @SubscribeEvent
    public static void chunkLoaded(ChunkDataEvent.Load event) {
        if (!Tc4WandBridge.available()) return;
        CompoundTag data = event.getData();
        readBlockEntities(data.getList("block_entities", Tag.TAG_COMPOUND));
        if (data.contains("Level", Tag.TAG_COMPOUND))
            readBlockEntities(data.getCompound("Level").getList("TileEntities", Tag.TAG_COMPOUND));
    }

    private static void readBlockEntities(ListTag tiles) {
        for (Tag value : tiles) if (value instanceof CompoundTag tile) restoreTile(tile);
    }

    public static void restoreTile(CompoundTag tile) {
        if (!tile.getString("mfcl_legacy_block").startsWith("Thaumcraft:") || !tile.contains("mfcl_legacy_nbt", Tag.TAG_BYTE_ARRAY)) return;
        byte[] bytes = tile.getByteArray("mfcl_legacy_nbt");
        if (bytes.length == 0) return;
        try { scan(NbtIo.readCompressed(new ByteArrayInputStream(bytes))); }
        catch (IOException | RuntimeException e) { LogUtils.getLogger().warn("Could not restore saved TC4 wand definitions", e); }
    }

    private static void scan(Tag value) {
        if (value instanceof CompoundTag compound) {
            if (compound.contains("rod", Tag.TAG_STRING) && compound.contains("cap", Tag.TAG_STRING))
                Tc4WandBridge.restoreDefinitions(compound);
            for (String key : compound.getAllKeys()) scan(compound.get(key));
        } else if (value instanceof ListTag list) {
            for (Tag element : list) scan(element);
        }
    }
}
