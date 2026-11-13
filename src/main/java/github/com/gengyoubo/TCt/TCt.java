package github.com.gengyoubo.TCt;

import github.com.gengyoubo.TCt.item.TCtItems;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvil;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvilConfig;
import github.com.gengyoubo.TCt.compat.tc4.Tc4WandBridge;
import github.com.gengyoubo.TCt.stats.WandStatTypes;
import github.com.gengyoubo.TCt.stats.WandToolStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TCt.MOD_ID)
public class TCt {
    //神秘工匠
    public static final String MOD_ID = "tc_t";

    public TCt() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        WandToolStats.init();
        Tc4WandBridge.registerAdapter();
        ArcaneAnvil.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ArcaneAnvilConfig.SPEC);
        TCtItems.register(modBus);
        modBus.addListener(TCt::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(WandStatTypes::register);
    }

    public static ResourceLocation getResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
