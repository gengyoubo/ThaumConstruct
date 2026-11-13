package github.com.gengyoubo.TCt.item;

import github.com.gengyoubo.TCt.arcane.StaffRodCasting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.materials.stats.MaterialStatsId;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import javax.annotation.Nullable;
import java.util.List;

/** A material-bearing casting that cannot be used as a staff part until awakened. */
public final class StaffRodBlankItem extends ToolPartItem {
    public StaffRodBlankItem(Properties properties, MaterialStatsId stat) { super(properties, stat); }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, level, lines, flag);
        lines.add(Component.translatable(stack.getOrCreateTag().getBoolean(StaffRodCasting.ATTUNED)
                ? "tooltip.tc_t.staff_rod_blank.wand" : "tooltip.tc_t.staff_rod_blank.charm").withStyle(ChatFormatting.GOLD));
    }
}
