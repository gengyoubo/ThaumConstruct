package github.com.gengyoubo.TCt.client;

import github.com.gengyoubo.TCt.TCt;
import github.com.gengyoubo.TCt.arcane.ArcaneAnvil;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TCt.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArcaneClientSetup {
    private ArcaneClientSetup() { }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ArcaneAnvil.MENU.get(), ArcaneAnvilScreen::new));
    }
}
