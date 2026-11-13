package github.com.gengyoubo.TCt.compat.tc4;

import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.mfcl.api.LegacyMappings;
import github.com.gengyoubo.mfcl.api.LegacyObject;
import github.com.gengyoubo.mfcl.api.MfclApi;
import github.com.gengyoubo.mfcl.api.mla.LegacyConstructor;
import github.com.gengyoubo.mfcl.api.mla.LegacyMethod;
import github.com.gengyoubo.mfcl.api.mla.LegacyType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

/** Server-side, copy-on-write TC4 crafting-vis check and consumption. Costs are in centivis. */
public final class ArcaneVis {
    private ArcaneVis() { }

    @LegacyType("net.minecraft.item.ItemStack")
    public interface OldStack {
        @LegacyMethod(name = "func_77973_b", descriptor = "()Lnet/minecraft/item/Item;") LegacyObject item();
    }

    @LegacyType("thaumcraft.api.aspects.Aspect")
    public interface OldAspects {
        @LegacyMethod(name = "getAspect", descriptor = "(Ljava/lang/String;)Lthaumcraft/api/aspects/Aspect;", isStatic = true)
        LegacyObject get(String tag);
    }

    @LegacyType("thaumcraft.api.aspects.AspectList")
    public interface ListFactory {
        @LegacyConstructor(descriptor = "()V") LegacyObject create();
    }

    @LegacyType("thaumcraft.api.aspects.AspectList")
    public interface ListBuilder {
        @LegacyMethod(name = "add", descriptor = "(Lthaumcraft/api/aspects/Aspect;I)Lthaumcraft/api/aspects/AspectList;")
        LegacyObject add(LegacyObject aspect, int amount);
    }

    @LegacyType("thaumcraft.api.ThaumcraftApiHelper")
    public interface VisHelper {
        @LegacyMethod(name = "consumeVisFromWand", descriptor = "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Lthaumcraft/api/aspects/AspectList;ZZ)Z", isStatic = true)
        boolean consume(LegacyObject wand, LegacyObject player, LegacyObject cost, boolean actual, boolean crafting);
    }

    /**
     * Returns a new wand when the player can pay. The caller must revalidate its inputs and write the returned
     * stack back only when taking the output; this method never changes the supplied stack.
     */
    public static Optional<ItemStack> pay(ServerPlayer player, ItemStack wand, Map<Aspect, Integer> costs, boolean simulate) {
        if (wand.isEmpty() || costs.isEmpty() || costs.values().stream().anyMatch(value -> value == null || value <= 0)) {
            return Optional.empty();
        }
        var runtime = MfclApi.runtime().orElse(null);
        if (runtime == null || !runtime.loadedMods().contains("Thaumcraft")) return Optional.empty();
        var old = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, wand.copy()).orElse(null);
        if (old == null) return Optional.empty();
        var item = runtime.accessor(OldStack.class, old).item();
        if (item == null || !runtime.values().isInstance(item, "thaumcraft.common.items.wands.ItemWandCasting")) {
            return Optional.empty();
        }
        var list = runtime.accessor(ListFactory.class).create();
        var builder = runtime.accessor(ListBuilder.class, list);
        var lookup = runtime.accessor(OldAspects.class);
        for (var entry : costs.entrySet()) {
            if (entry.getKey() == null) return Optional.empty();
            var aspect = lookup.get(entry.getKey().getId());
            if (aspect == null) return Optional.empty();
            builder.add(aspect, entry.getValue());
        }
        boolean paid = runtime.withPlayer(player,
                oldPlayer -> runtime.accessor(VisHelper.class).consume(old, oldPlayer, list, !simulate, true));
        if (!paid) return Optional.empty();
        return simulate ? Optional.of(wand.copy()) : runtime.mappings().toHost(LegacyMappings.ITEM_STACK, old, ItemStack.class);
    }
}
