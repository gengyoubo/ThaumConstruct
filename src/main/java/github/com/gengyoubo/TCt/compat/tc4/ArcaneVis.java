package github.com.gengyoubo.TCt.compat.tc4;

import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.mfcl.api.LegacyMappings;
import github.com.gengyoubo.mfcl.api.MfclApi;
import github.com.gengyoubo.tcmodern.api.wand.TcWandVis;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Adapts TCt's aspect keys and native stacks to TC Modern Lib's copy-on-write Vis API. */
public final class ArcaneVis {
    private ArcaneVis() { }

    public static Optional<ItemStack> pay(ServerPlayer player, ItemStack wand, Map<Aspect, Integer> costs, boolean simulate) {
        if (wand.isEmpty() || !Tc4WandBridge.canCraftWith(wand) || costs == null || costs.isEmpty()) return Optional.empty();
        var tags = new LinkedHashMap<String, Integer>();
        for (var entry : costs.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) return Optional.empty();
            tags.put(entry.getKey().getId(), entry.getValue());
        }
        var runtime = MfclApi.runtime().orElse(null);
        if (runtime == null || !runtime.loadedMods().contains("Thaumcraft")) return Optional.empty();
        var old = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, wand.copy()).orElse(null);
        if (old == null) return Optional.empty();
        var payment = TcWandVis.consume(runtime, player, runtime.snapshot(old), tags, simulate, true);
        if (!payment.success()) return Optional.empty();
        if (simulate) return Optional.of(wand.copy());
        return Optional.of(runtime.mappings().applyTo(LegacyMappings.ITEM_STACK, wand, runtime.stack(payment.stack())));
    }
}
