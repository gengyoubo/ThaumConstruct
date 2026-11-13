package github.com.gengyoubo.TCt.item;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.stats.WandCapStats;
import github.com.gengyoubo.TCt.stats.WandRodStats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.CastItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.part.ToolPartItem;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.TinkerToolParts;

import java.util.function.Consumer;

public final class TCtItems {
    private TCtItems() {}

    public static final ItemDeferredRegisterExtension ITEMS = new ItemDeferredRegisterExtension(TCt.MOD_ID);

    /** 杖柄 */
    public static final ItemObject<ToolPartItem> WAND_ROD =
            ITEMS.register("wand_rod", () -> new ToolPartItem(new Item.Properties(), WandRodStats.ID));
    /** 杖端 */
    public static final ItemObject<ToolPartItem> WAND_CAP =
            ITEMS.register("wand_cap", () -> new ToolPartItem(new Item.Properties(), WandCapStats.ID));

    /** 金铸模 / 沙铸模 / 红沙铸模 */
    public static final CastItemObject WAND_ROD_CAST = ITEMS.registerCast(WAND_ROD, new Item.Properties());
    public static final CastItemObject WAND_CAP_CAST = ITEMS.registerCast(WAND_CAP, new Item.Properties());

    /** 法杖：杖柄 + 两个杖端，属性见 data/tc_t/tinkering/tool_definitions/wand.json */
    public static final ToolDefinition WAND_DEFINITION = ToolDefinition.create(TCt.getResource("wand"));
    public static final ItemObject<WandItem> WAND =
            ITEMS.register("wand", () -> new WandItem(new Item.Properties().stacksTo(1), WAND_DEFINITION));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(TCtItems::addCreativeTabItems);
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        Consumer<ItemStack> output = event::accept;
        if (event.getTabKey() == TinkerTools.tabTools.getKey()) {
            ToolBuildHandler.addVariants(output, WAND.get(), "");
        } else if (event.getTabKey() == TinkerToolParts.tabToolParts.getKey()) {
            WAND_ROD.get().addVariants(output, "");
            WAND_CAP.get().addVariants(output, "");
        } else if (event.getTabKey() == TinkerSmeltery.tabSmeltery.getKey()) {
            for (CastItemObject cast : new CastItemObject[]{WAND_ROD_CAST, WAND_CAP_CAST}) {
                event.accept(cast.get());
                event.accept(cast.getSand());
                event.accept(cast.getRedSand());
            }
        }
    }
}
