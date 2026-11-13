package github.com.gengyoubo.TCt.compat.tc4;

import github.com.gengyoubo.mfcl.api.LegacyObject;
import github.com.gengyoubo.mfcl.api.LegacyRedirects;
import github.com.gengyoubo.mfcl.api.MfclApi;
import github.com.gengyoubo.mfcl.api.mla.LegacyMethod;
import github.com.gengyoubo.mfcl.api.mla.LegacyType;
import github.com.gengyoubo.mfcl.api.spi.LegacyRedirectProvider;

/** Applies per-aspect capacity inside TC4's charging calls, including simulation calls. */
public final class Tc4WandCapacityRedirect implements LegacyRedirectProvider {
    private static final String WAND = "thaumcraft.common.items.wands.ItemWandCasting";
    private static final String DESC = "(Lnet/minecraft/item/ItemStack;Lthaumcraft/api/aspects/Aspect;IZ)I";

    @LegacyType("thaumcraft.api.aspects.Aspect")
    public interface OldAspect {
        @LegacyMethod(name = "getTag", descriptor = "()Ljava/lang/String;") String tag();
        @LegacyMethod(name = "isPrimal", descriptor = "()Z") boolean isPrimal();
    }

    @LegacyType(WAND)
    public interface OldWand {
        @LegacyMethod(name = "getVis", descriptor = "(Lnet/minecraft/item/ItemStack;Lthaumcraft/api/aspects/Aspect;)I")
        int getVis(LegacyObject stack, LegacyObject aspect);
        @LegacyMethod(name = "storeVis", descriptor = "(Lnet/minecraft/item/ItemStack;Lthaumcraft/api/aspects/Aspect;I)V")
        void storeVis(LegacyObject stack, LegacyObject aspect, int amount);
    }

    @Override public void register(LegacyRedirects redirects) {
        redirects.redirect("thaumcraft.", WAND, "addVis", DESC, call -> add(call, 100));
        redirects.redirect("thaumcraft.", WAND, "addRealVis", DESC, call -> add(call, 1));
    }

    private static Object add(LegacyRedirects.Call call, int unit) throws Throwable {
        var runtime = MfclApi.runtime().orElse(null);
        Object[] args = call.arguments();
        if (runtime == null || !(args[0] instanceof LegacyObject stack)
                || !(args[1] instanceof LegacyObject aspect) || call.receiver() == null) return call.proceed(args);
        var raw = runtime.accessor(Tc4WandBridge.OldStack.class, stack).tag();
        if (raw == null) return call.proceed(args);
        var nbt = runtime.accessor(Tc4WandBridge.OldTag.class, raw);
        var oldAspect = runtime.accessor(OldAspect.class, aspect);
        String key = Tc4WandBridge.CAPACITY_PREFIX + oldAspect.tag();
        if (!nbt.contains(key) || !oldAspect.isPrimal()) return call.proceed(args);
        int amount = (Integer) args[2];
        if (amount < 0) return call.proceed(args);
        var wand = runtime.accessor(OldWand.class, call.receiver());
        int current = wand.getVis(stack, aspect);
        int max = Math.max(0, nbt.getInt(key));
        long result = (long) current + (long) amount * unit;
        if ((Boolean) args[3]) wand.storeVis(stack, aspect, (int) Math.min(result, max));
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, result - max) / unit);
    }
}
