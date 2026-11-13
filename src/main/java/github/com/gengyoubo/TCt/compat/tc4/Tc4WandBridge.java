package github.com.gengyoubo.TCt.compat.tc4;

import com.mojang.logging.LogUtils;
import github.com.gengyoubo.TCt.item.WandItem;
import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.TCt.stats.WandToolStats;
import github.com.gengyoubo.mfcl.api.LegacyMappings;
import github.com.gengyoubo.mfcl.api.LegacyObject;
import github.com.gengyoubo.mfcl.api.LegacyRuntime;
import github.com.gengyoubo.mfcl.api.LegacyStack;
import github.com.gengyoubo.mfcl.api.LegacyStackAdapters;
import github.com.gengyoubo.mfcl.api.MfclApi;
import github.com.gengyoubo.mfcl.api.mla.LegacyConstructor;
import github.com.gengyoubo.mfcl.api.mla.LegacyField;
import github.com.gengyoubo.mfcl.api.mla.LegacyMethod;
import github.com.gengyoubo.mfcl.api.mla.LegacyType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * 把法杖转交给 MFCL 里的神秘时代 4 处理：临时生成一根同样杖柄、杖端、魔力的 TC4 法杖（Thaumcraft:WandCasting），
 * 调用它的原版逻辑，结束后把魔力写回我们的法杖。只有装了 MFCL 时才会加载这个类。
 */
public final class Tc4WandBridge {
    private Tc4WandBridge() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String WAND_KEY = "Thaumcraft:WandCasting";

    private static final Set<String> REGISTERED_CAPS = new HashSet<>();
    /** 本次运行里已经在 TC4 注册过的自定义杖柄 */
    private static final Set<String> REGISTERED_RODS = new HashSet<>();
    private static final String LEGACY_WAND_DATA = "tc_t_legacy_wand";
    private static final String HOST_WAND_DATA = "tc_t_host_wand";
    public static final String CAPACITY_PREFIX = "tc_t_capacity_";

    @LegacyType("thaumcraft.api.wands.WandRod")
    public interface WandRodFactory {
        @LegacyConstructor(descriptor = "(Ljava/lang/String;ILnet/minecraft/item/ItemStack;I)V")
        LegacyObject create(String tag, int capacity, LegacyObject item, int craftCost);
    }

    @LegacyType("thaumcraft.api.wands.StaffRod")
    public interface StaffRodFactory {
        @LegacyConstructor(descriptor = "(Ljava/lang/String;ILnet/minecraft/item/ItemStack;I)V")
        LegacyObject create(String tag, int capacity, LegacyObject item, int craftCost);
    }

    @LegacyType("thaumcraft.common.items.wands.ItemWandCasting")
    public interface OldWand {
        @LegacyMethod(name = "isStaff", descriptor = "(Lnet/minecraft/item/ItemStack;)Z") boolean isStaff(LegacyObject stack);
        @LegacyMethod(name = "isSceptre", descriptor = "(Lnet/minecraft/item/ItemStack;)Z") boolean isSceptre(LegacyObject stack);
        @LegacyMethod(name = "getMaxVis", descriptor = "(Lnet/minecraft/item/ItemStack;)I") int getMaxVis(LegacyObject stack);
    }

    @LegacyType("thaumcraft.api.wands.WandCap")
    public interface WandCapFactory {
        @LegacyConstructor(descriptor = "(Ljava/lang/String;FLjava/util/List;FLnet/minecraft/item/ItemStack;I)V")
        LegacyObject create(String tag, float baseCost, LegacyObject aspects, float specialCost, LegacyObject item, int craftCost);
    }

    @LegacyType("thaumcraft.api.wands.WandCap")
    public interface WandCaps {
        @LegacyField(name = "caps", descriptor = "Ljava/util/LinkedHashMap;", isStatic = true)
        LegacyObject all();
    }

    @LegacyType("net.minecraft.item.ItemStack")
    public interface OldStack {
        @LegacyField(name = "field_77990_d", descriptor = "Lnet/minecraft/nbt/NBTTagCompound;")
        LegacyObject tag();
    }

    @LegacyType("net.minecraft.nbt.NBTTagCompound")
    public interface OldTag {
        @LegacyMethod(name = "func_74778_a", descriptor = "(Ljava/lang/String;Ljava/lang/String;)V")
        void putString(String key, String value);
        @LegacyMethod(name = "func_74768_a", descriptor = "(Ljava/lang/String;I)V")
        void putInt(String key, int value);
        @LegacyMethod(name = "func_74762_e", descriptor = "(Ljava/lang/String;)I")
        int getInt(String key);
        @LegacyMethod(name = "func_74779_i", descriptor = "(Ljava/lang/String;)Ljava/lang/String;")
        String getString(String key);
        @LegacyMethod(name = "func_82580_o", descriptor = "(Ljava/lang/String;)V")
        void remove(String key);
        @LegacyMethod(name = "func_74764_b", descriptor = "(Ljava/lang/String;)Z")
        boolean contains(String key);
    }

    /** MFCL v3 uses this representation for rendering, inventories and original gameplay. */
    public static void registerAdapter() {
        LegacyStackAdapters.register(new LegacyStackAdapters.Adapter() {
            @Override public LegacyStack toLegacy(Object value) {
                if (!(value instanceof ItemStack wand) || !(wand.getItem() instanceof WandItem) || !available()) return null;
                LegacyRuntime runtime = runtime();
                ItemStack host = Tc4WandBridge.toHost(wand);
                LegacyObject legacy = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, host).orElseThrow();
                OldTag tag = runtime.accessor(OldTag.class, runtime.accessor(OldStack.class, legacy).tag());
                tag.putString(HOST_WAND_DATA, wand.save(new CompoundTag()).toString());
                return runtime.snapshot(legacy);
            }

            @Override public Object toHost(LegacyStack snapshot) {
                if (!WAND_KEY.equals(snapshot.key()) || !snapshot.snbt().contains(HOST_WAND_DATA)) return null;
                LegacyRuntime runtime = runtime();
                LegacyObject legacy = runtime.stack(snapshot);
                LegacyObject rawTag = runtime.accessor(OldStack.class, legacy).tag();
                if (rawTag == null) return null;
                OldTag tag = runtime.accessor(OldTag.class, rawTag);
                String saved = tag.getString(HOST_WAND_DATA);
                if (saved.isEmpty()) return null;
                try {
                    ItemStack wand = ItemStack.of(TagParser.parseTag(saved));
                    if (!(wand.getItem() instanceof WandItem)) return null;
                    wand.setCount(snapshot.count());
                    tag.remove(HOST_WAND_DATA);
                    ItemStack host = runtime.mappings().toHost(LegacyMappings.ITEM_STACK, legacy, ItemStack.class).orElseThrow();
                    fromHost(host, wand);
                    return wand;
                } catch (CommandSyntaxException e) {
                    throw new IllegalArgumentException("Invalid saved host wand", e);
                }
            }
        });
    }

    public static boolean available() {
        return MfclApi.runtime().map(runtime -> runtime.loadedMods().contains("Thaumcraft")).orElse(false);
    }

    private static LegacyRuntime runtime() {
        return MfclApi.runtime().orElseThrow();
    }

    /** 读取一个 1.20 物品对应的 TC4 物品快照（键、元数据、NBT） */
    @Nullable
    private static LegacyStack legacySnapshot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        LegacyRuntime runtime = runtime();
        return runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, stack).map(runtime::snapshot).orElse(null);
    }

    public static boolean isHostWand(ItemStack stack) {
        try {
            LegacyStack snapshot = legacySnapshot(stack);
            return snapshot != null && WAND_KEY.equals(snapshot.key());
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 把玩家手里的法杖临时换成 TC4 法杖执行 action，结束后同步魔力并换回。
     * MFCL 可能直接改写玩家手里的物品，所以结束时优先读取手里的 TC4 法杖。
     */
    public static <T> T withHost(@Nullable Player player, InteractionHand hand, ItemStack wand, Function<ItemStack, T> action) {
        ItemStack host = toHost(wand);
        if (player != null) {
            player.setItemInHand(hand, host);
        }
        try {
            return action.apply(host);
        } finally {
            ItemStack after = host;
            if (player != null) {
                ItemStack held = player.getItemInHand(hand);
                if (isHostWand(held)) {
                    after = held;
                }
                player.setItemInHand(hand, wand);
            }
            fromHost(after, wand);
        }
    }

    /** 生成对应的 TC4 法杖：{rod, cap, 六种要素的魔力（百分之一单位）} */
    public static ItemStack toHost(ItemStack wand) {
        ToolStack tool = WandItem.toolWithAspectStats(wand);
        MaterialNBT materials = tool.getMaterials();
        int capacity = 0;
        for (Aspect aspect : Aspect.values()) capacity = Math.max(capacity, WandToolStats.capacity(tool.getStats(), aspect));
        String saved = wand.getOrCreateTag().getString(LEGACY_WAND_DATA);
        LegacyRuntime runtime = runtime();
        LegacyObject legacy = runtime.stack(new LegacyStack(WAND_KEY, 0, 1, saved.isEmpty() ? "{}" : saved));
        OldTag tag = runtime.accessor(OldTag.class, runtime.accessor(OldStack.class, legacy).tag());
        WandItem.Kind kind = ((WandItem) wand.getItem()).kind();
        int rodCapacity = kind == WandItem.Kind.SCEPTRE ? Math.round(capacity / 1.5f) : capacity;
        tag.putString("rod", rodTag(material(materials, 0), rodCapacity, kind == WandItem.Kind.STAFF));
        if (kind == WandItem.Kind.SCEPTRE) tag.putInt("sceptre", 1); else tag.remove("sceptre");
        tag.putString("cap", capTag(tool));
        for (Aspect aspect : Aspect.values()) {
            tag.putInt(CAPACITY_PREFIX + aspect.getId(), WandToolStats.capacity(tool.getStats(), aspect) * 100);
            tag.putInt(aspect.getId(), tool.getPersistentData().getInt(WandItem.visKey(aspect)));
        }
        // 和 tc4modern 一样走公开接口：先在旧版里造出 ItemStack，再映射成 1.20 物品
        return runtime.mappings().toHost(LegacyMappings.ITEM_STACK, legacy, ItemStack.class).orElseThrow();
    }

    /** A TC4 staff cannot supply an arcane workbench. */
    public static boolean canCraftWith(ItemStack stack) {
        try {
            if (!isHostWand(stack)) return false;
            var runtime = runtime();
            var legacy = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, stack).orElse(null);
            if (legacy == null) return false;
            var item = runtime.accessor(ArcaneVis.OldStack.class, legacy).item();
            return item != null && !runtime.accessor(OldWand.class, item).isStaff(legacy);
        } catch (RuntimeException ex) { return false; }
    }

    /** The casting-table awakening is performed with a wand, not a staff or crafting sceptre. */
    public static boolean isNormalWand(ItemStack stack) {
        try {
            if (!isHostWand(stack)) return false;
            var runtime = runtime();
            var legacy = runtime.mappings().toLegacy(LegacyMappings.ITEM_STACK, stack).orElse(null);
            if (legacy == null) return false;
            var item = runtime.accessor(ArcaneVis.OldStack.class, legacy).item();
            if (item == null) return false;
            var wand = runtime.accessor(OldWand.class, item);
            return !wand.isStaff(legacy) && !wand.isSceptre(legacy);
        } catch (RuntimeException ex) { return false; }
    }

    /** 读取 TC4 法杖里的魔力写回我们的法杖，不超过我们自己的容量 */
    public static void fromHost(ItemStack host, ItemStack wand) {
        try {
            LegacyStack snapshot = legacySnapshot(host);
            if (snapshot == null || snapshot.snbt() == null || snapshot.snbt().isEmpty()) return;
            LegacyRuntime runtime = runtime();
            LegacyObject legacy = runtime.stack(snapshot);
            LegacyObject rawTag = runtime.accessor(OldStack.class, legacy).tag();
            if (rawTag == null) return;
            OldTag data = runtime.accessor(OldTag.class, rawTag);
            ToolStack tool = WandItem.toolWithAspectStats(wand);
            for (Aspect aspect : Aspect.values()) {
                int max = WandToolStats.capacity(tool.getStats(), aspect) * 100;
                int value = Math.max(0, Math.min(max, data.getInt(aspect.getId())));
                tool.getPersistentData().putInt(WandItem.visKey(aspect), value);
            }
            // Keep old-format SNBT intact: 1.7.10 lists are not valid modern SNBT.
            wand.getOrCreateTag().putString(LEGACY_WAND_DATA, snapshot.snbt());
        } catch (RuntimeException e) {
            LOG.warn("Failed to read TC4 wand state", e);
        }
    }

    /* ---------- 右键相关的转交 ---------- */

    /** 对着方块右键（TC4 的书架/工作台/坩埚转换、多方块结构都在 onItemUseFirst 里） */
    public static InteractionResult onItemUseFirst(ItemStack wand, UseOnContext context) {
        InteractionResult result = withHost(context.getPlayer(), context.getHand(), wand,
                host -> host.getItem().onItemUseFirst(host, hostContext(context, host)));
        resumeUse(context.getPlayer(), context.getHand());
        return result;
    }

    public static InteractionResult useOn(ItemStack wand, UseOnContext context) {
        InteractionResult result = withHost(context.getPlayer(), context.getHand(), wand,
                host -> host.getItem().useOn(hostContext(context, host)));
        resumeUse(context.getPlayer(), context.getHand());
        return result;
    }

    /** 对着空气右键（核心施法、开始汲取节点） */
    public static InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);
        InteractionResult result = withHost(player, hand, wand, host -> {
            InteractionResultHolder<ItemStack> holder = host.getItem().use(level, player, hand);
            if (isHostWand(holder.getObject())) {
                player.setItemInHand(hand, holder.getObject());
            }
            return holder.getResult();
        });
        // TC4 开始持续使用时，使用中的物品是临时 TC4 法杖；换回后让玩家改为持续使用我们的法杖
        resumeUse(player, hand);
        return new InteractionResultHolder<>(result, wand);
    }

    private static void resumeUse(@Nullable Player player, InteractionHand hand) {
        if (player != null && player.isUsingItem() && player.getUsedItemHand() == hand && isHostWand(player.getUseItem())) {
            player.stopUsingItem();
            player.startUsingItem(hand);
        }
    }

    public static void onUseTick(Level level, LivingEntity entity, ItemStack wand, int remaining) {
        if (entity instanceof Player player) {
            withHost(player, player.getUsedItemHand(), wand, host -> {
                host.getItem().onUseTick(level, player, host, remaining);
                return null;
            });
        }
    }

    public static void releaseUsing(ItemStack wand, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            withHost(player, player.getUsedItemHand(), wand, host -> {
                host.getItem().releaseUsing(host, level, player, timeLeft);
                return null;
            });
        }
    }

    public static int getUseDuration(ItemStack wand) {
        ItemStack host = toHost(wand);
        return host.getItem().getUseDuration(host);
    }

    public static UseAnim getUseAnimation(ItemStack wand) {
        ItemStack host = toHost(wand);
        return host.getItem().getUseAnimation(host);
    }

    private static UseOnContext hostContext(UseOnContext context, ItemStack host) {
        BlockHitResult hit = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), context.isInside());
        return new UseOnContext(context.getLevel(), context.getPlayer(), context.getHand(), host, hit) {};
    }

    @Nullable
    private static MaterialVariantId material(MaterialNBT materials, int index) {
        return index < materials.size() ? materials.get(index).getVariant() : null;
    }

    /** TC4 sees the same averaged cap costs as the two Tinkers parts, including copper's special aspects. */
    private static String capTag(ToolStack tool) {
        int[] costs = new int[Aspect.values().length];
        Aspect[] aspects = Aspect.values();
        StringBuilder id = new StringBuilder("tc_t_c");
        for (int i = 0; i < aspects.length; i++) {
            costs[i] = Math.round(tool.getStats().get(WandToolStats.VIS_COST.get(aspects[i])) * 10000);
            id.append('_').append(costs[i]);
        }
        String tag = id.toString();
        synchronized (REGISTERED_CAPS) {
            if (!REGISTERED_CAPS.contains(tag)) {
                LegacyRuntime runtime = runtime();
                int base = costs[0];
                int special = base;
                var specialAspects = new ArrayList<LegacyObject>();
                for (int i = 1; i < costs.length; i++) {
                    if (costs[i] != base) {
                        if (special != base && costs[i] != special) {
                            throw new IllegalStateException("TC4 cap cannot represent more than two aspect costs");
                        }
                        special = costs[i];
                        specialAspects.add(runtime.accessor(ArcaneVis.OldAspects.class).get(aspects[i].getId()));
                    }
                }
                LegacyObject list = runtime.values().newList(specialAspects);
                LegacyObject item = runtime.stack(new LegacyStack("minecraft:stick", 0, 1, ""));
                runtime.accessor(WandCapFactory.class).create(tag, base / 10000f, list, special / 10000f, item, 1);
                REGISTERED_CAPS.add(tag);
            }
        }
        return tag;
    }

    /** 木杖柄直接用 TC4 的 wood；其他材料按容量注册一个自定义 TC4 杖柄（tc_t_<容量>） */
    private static String rodTag(@Nullable MaterialVariantId material, int capacity, boolean staff) {
        if (!staff && material != null && material.getId().toString().equals("tc_t:silverwood") && capacity == 100) return "silverwood";
        if (!staff && material != null && material.getId().toString().equals("tconstruct:wood") && capacity == 25) {
            return "wood";
        }
        String tag = "tc_t_" + (staff ? "staff_" : "") + capacity;
        synchronized (REGISTERED_RODS) {
            if (!REGISTERED_RODS.contains(tag)) {
                try {
                    LegacyRuntime runtime = MfclApi.runtime().orElseThrow();
                    LegacyObject stick = runtime.stack(new LegacyStack("minecraft:stick", 0, 1, ""));
                    if (staff) runtime.accessor(StaffRodFactory.class).create(tag, capacity, stick, 1);
                    else runtime.accessor(WandRodFactory.class).create(tag, capacity, stick, 1);
                    REGISTERED_RODS.add(tag);
                } catch (RuntimeException e) {
                    if (staff) throw new IllegalStateException("Failed to register TC4 staff rod " + tag, e);
                    LOG.warn("Failed to register TC4 wand rod {}, falling back to wood", tag, e);
                    return "wood";
                }
            }
        }
        return staff ? tag + "_staff" : tag;
    }
}
