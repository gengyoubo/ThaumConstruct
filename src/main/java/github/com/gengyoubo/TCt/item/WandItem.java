package github.com.gengyoubo.TCt.item;

import github.com.gengyoubo.TCt.TCt;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import github.com.gengyoubo.TCt.client.WandClientExtension;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import github.com.gengyoubo.TCt.stats.Aspect;
import github.com.gengyoubo.TCt.stats.WandCapStats;
import github.com.gengyoubo.TCt.stats.WandToolStats;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.ModList;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.library.tools.helper.TooltipUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 法杖：匠魂工具，没有耐久。按 TC4 的样子命名（铁杖端木质法杖）并显示魔力容量、平均消耗和储存的要素。
 * 客户端用 3D 渲染器绘制。
 */
public class WandItem extends ModifiableItem {
    public enum Kind { WAND, SCEPTRE, STAFF }
    private final Kind kind;

    public WandItem(Properties properties, ToolDefinition definition) { this(properties, definition, Kind.WAND); }
    public WandItem(Properties properties, ToolDefinition definition, Kind kind) {
        super(properties, definition);
        this.kind = kind;
    }
    public Kind kind() { return kind; }
    public int firstCapIndex() { return 1; }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> original = super.getAttributeModifiers(slot, stack);
        if (kind != Kind.STAFF || slot != EquipmentSlot.MAINHAND) return original;
        var attributes = ImmutableMultimap.<Attribute, AttributeModifier>builder().putAll(original);
        if (!original.containsKey(Attributes.ATTACK_DAMAGE))
            attributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                    "Staff weapon damage", 6.0, AttributeModifier.Operation.ADDITION));
        if (!original.containsKey(Attributes.ATTACK_SPEED))
            attributes.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "Staff attack speed", -2.4, AttributeModifier.Operation.ADDITION));
        return attributes.build();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(WandClientExtension.INSTANCE);
    }

    /** 储存的魔力，保存在工具的持久数据里，键为 tc_t:vis_<要素>，单位和 TC4 一样是 1/100 */
    public static ResourceLocation visKey(Aspect aspect) {
        return TCt.getResource("vis_" + aspect.getId());
    }

    /** Rebuild pre-existing wands once so their saved stats include the new aspect capacities. */
    public static ToolStack toolWithAspectStats(ItemStack stack) {
        ToolStack tool = ToolStack.from(stack);
        var stats = stack.getTagElement("tic_stats");
        if (ToolStack.isInitialized(stack) && ((WandItem) stack.getItem()).getToolDefinition().isDataLoaded()
                && ((WandItem) stack.getItem()).kind() == Kind.STAFF && tool.getMaterials().size() == 4) {
            var old = tool.getMaterials();
            tool.setMaterials(MaterialNBT.of(old.get(0), old.get(2), old.get(3)));
        }
        if (ToolStack.isInitialized(stack) && ((WandItem) stack.getItem()).getToolDefinition().isDataLoaded() && stats != null
                && (!stats.contains("tc_t:vis_capacity_aer") || stack.getOrCreateTag().getInt("tc_t_wand_stats_version") < 2)) {
            tool.rebuildStats();
            stack.getOrCreateTag().putInt("tc_t_wand_stats_version", 2);
        }
        return tool;
    }

    /* 右键：装了 MFCL 和神秘时代 4 时转交给 TC4 的法杖逻辑，否则按普通匠魂工具处理 */

    private static final boolean MFCL = ModList.get().isLoaded("mfcl");

    private static boolean tc4() {
        return MFCL && Tc4WandBridge.available();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (tc4()) {
            InteractionResult result = Tc4WandBridge.onItemUseFirst(stack, context);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.onItemUseFirst(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (tc4()) {
            InteractionResult result = Tc4WandBridge.useOn(context.getItemInHand(), context);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (tc4()) {
            InteractionResultHolder<ItemStack> result = Tc4WandBridge.use(level, player, hand);
            if (result.getResult() != InteractionResult.PASS) {
                return result;
            }
        }
        return super.use(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (tc4()) {
            Tc4WandBridge.onUseTick(level, entity, stack, remaining);
        } else {
            super.onUseTick(level, entity, stack, remaining);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (tc4()) {
            Tc4WandBridge.releaseUsing(stack, level, entity, timeLeft);
        } else {
            super.releaseUsing(stack, level, entity, timeLeft);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return tc4() ? Tc4WandBridge.getUseDuration(stack) : super.getUseDuration(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return tc4() ? Tc4WandBridge.getUseAnimation(stack) : super.getUseAnimation(stack);
    }

    /* 名称：%杖端%杖柄%法杖，和 TC4 的 item.Wand.name=%CAP%ROD%OBJ 一致 */

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            return super.getName(stack);
        }
        MaterialNBT materials = ToolStack.isInitialized(stack)
                ? toolWithAspectStats(stack).getMaterials() : ToolStack.from(stack).getMaterials();
        if (materials.size() < 2) {
            return super.getName(stack);
        }
        int capIndex = firstCapIndex();
        if (materials.size() <= capIndex) return super.getName(stack);
        return Component.translatable("item.tc_t." + (kind == Kind.WAND ? "wand" : kind == Kind.SCEPTRE ? "sceptre" : "staff") + ".format",
                partName("cap", materials.get(capIndex).getVariant()),
                partName("rod", materials.get(0).getVariant()));
    }

    /** 优先用 item.tc_t.wand.<部件>.<命名空间>.<材料>[.<变种>]，否则用 item.tc_t.wand.<部件>.format 套材料名 */
    private static Component partName(String part, MaterialVariantId material) {
        Language language = Language.getInstance();
        String base = "item.tc_t.wand." + part + "." + material.getId().getNamespace() + "." + material.getId().getPath();
        if (material.hasVariant() && language.has(base + "." + material.getVariant())) {
            return Component.translatable(base + "." + material.getVariant());
        }
        if (!material.hasVariant() && language.has(base)) {
            return Component.translatable(base);
        }
        return Component.translatable("item.tc_t.wand." + part + ".format", materialName(material));
    }

    private static Component materialName(MaterialVariantId material) {
        String base = "material." + material.getId().getNamespace() + "." + material.getId().getPath();
        if (material.hasVariant() && Language.getInstance().has(base + "." + material.getVariant())) {
            return Component.translatable(base + "." + material.getVariant());
        }
        return Component.translatable(base);
    }

    /* 提示：容量 25 (110% 平均魔力消耗)，下一行是六种要素当前储存量 */

    private static final DecimalFormat VIS_FORMAT = new DecimalFormat("#######.##");

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltips, TooltipFlag flag) {
        if (ToolStack.isInitialized(stack)) toolWithAspectStats(stack);
        int start = tooltips.size();
        super.appendHoverText(stack, level, tooltips, flag);
        if (SafeClientAccess.getTooltipKey() == TooltipKey.NORMAL && getToolDefinition().isDataLoaded()
                && ToolStack.isInitialized(stack) && !TooltipUtil.isDisplay(stack)) {
            List<Component> wandInfo = new ArrayList<>();
            getStatInformation(toolWithAspectStats(stack), level == null ? null : SafeClientAccess.getPlayer(),
                    wandInfo, TooltipKey.NORMAL, flag);
            tooltips.addAll(start, wandInfo);
        }
        if (getToolDefinition().isDataLoaded() && ToolStack.isInitialized(stack)) {
            if (!tooltips.contains(TooltipUtil.TOOLTIP_HOLD_SHIFT)) {
                tooltips.add(TooltipUtil.TOOLTIP_HOLD_SHIFT);
            }
            if (!tooltips.contains(TooltipUtil.TOOLTIP_HOLD_CTRL)) {
                tooltips.add(TooltipUtil.TOOLTIP_HOLD_CTRL);
            }
        }
    }

    @Override
    public List<Component> getStatInformation(IToolStackView tool, @Nullable Player player, List<Component> tooltips,
                                              TooltipKey key, TooltipFlag flag) {
        StatsNBT stats = tool.getStats();
        int capacity = Math.round(stats.get(WandToolStats.VIS_CAPACITY));
        tooltips.add(Component.translatable("tooltip.tc_t.wand.capacity").withStyle(ChatFormatting.GOLD)
                .append(" ")
                .append(Component.literal(String.valueOf(capacity)).withStyle(ChatFormatting.GOLD))
                .append(" ")
                .append(Component.translatable("tooltip.tc_t.wand.average_cost",
                        WandCapStats.percent(Math.max(0.1f, WandToolStats.averageCost(stats) - (kind == Kind.SCEPTRE ? 0.1f : 0f)))).withStyle(ChatFormatting.WHITE)));

        MutableComponent vis = Component.empty();
        Aspect[] aspects = Aspect.values();
        for (int i = 0; i < aspects.length; i++) {
            Aspect aspect = aspects[i];
            if (i > 0) {
                vis.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
            }
            float amount = tool.getPersistentData().getInt(visKey(aspect)) / 100f;
            vis.append(Component.literal(VIS_FORMAT.format(amount) + "/" + WandToolStats.capacity(stats, aspect))
                    .withStyle(style -> style.withColor(aspect.getColor())));
        }
        tooltips.add(vis);
        return tooltips;
    }
}
