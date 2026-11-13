package github.com.gengyoubo.TCt;

import github.com.gengyoubo.TCt.item.TCtItems;
import github.com.gengyoubo.TCt.stats.WandPartStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TCt.MOD_ID)
public class TCt {
    public static final String MOD_ID = "tc_t";

    public TCt() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        TCtItems.register(modBus);
        modBus.addListener(TCt::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WandPartStats::registerStatTypes);
    }

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
