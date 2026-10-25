package dev.furq.resourcepackcached;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class FabricInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourcePackCached.boot(FabricLoader.getInstance().getGameDir(), FabricLoader.getInstance().getConfigDir());
    }
}
//?}
