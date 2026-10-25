package dev.furq.resourcepackcached;

//? if neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod("rpc")
public class NeoInit {
    public NeoInit(IEventBus eventBus, ModContainer modContainer) {
        eventBus.addListener(this::onClientSetup);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        ResourcePackCached.boot(FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get());
    }
}
*///?}
