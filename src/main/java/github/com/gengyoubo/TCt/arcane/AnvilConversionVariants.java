package github.com.gengyoubo.TCt.arcane;

import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tables.TinkerTables;

import java.util.ArrayList;
import java.util.List;

/** Creative material variants of both supported anvils and their matching conversion output. */
public final class AnvilConversionVariants {
    private AnvilConversionVariants() { }
    public record Variant(ItemStack source, ItemStack result) { }

    public static List<Variant> all() {
        var result = new ArrayList<Variant>();
        for (var block : List.of(TinkerTables.tinkersAnvil, TinkerTables.scorchedAnvil)) {
            ((IMaterialItem) block.asItem()).addVariants(source -> {
                ItemStack output = new ItemStack(ArcaneAnvil.ITEM.get());
                if (source.hasTag()) output.setTag(source.getTag().copy());
                result.add(new Variant(source.copyWithCount(1), output));
            }, "");
        }
        return result;
    }
}
