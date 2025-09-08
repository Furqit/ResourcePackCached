package dev.furq.resourcepackcached;

//? if fabric {
import net.fabricmc.api.ClientModInitializer;
//?} elif forge {
/*import net.minecraftforge.fml.common.Mod;
*///?} elif neoforge {
/*import net.neoforged.fml.common.Mod;
*///?}
import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.client.resources.server.ServerPackManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//? if forge || neoforge
/*@Mod("rpc")*/
public class ResourcePackCached /*? if fabric {*/ implements ClientModInitializer/*?}*/ {

    //? if fabric {
    @Override
    public void onInitializeClient() {
    //?} else {
    /*public ResourcePackCached() {*/
    //?}
        Map<UUID, Path> cachedPacks = CachingUtils.readCacheFile();
        if (!cachedPacks.isEmpty()) {
            try {
                CachingUtils.isStartup = true;
                DownloadedPackSource packSource = Minecraft.getInstance().getDownloadedPackSource();
                packSource.manager.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;

                cachedPacks.entrySet().stream()
                    .filter(entry -> Files.exists(entry.getValue()))
                    .forEach(entry -> packSource.pushLocalPack(entry.getKey(), entry.getValue()));

                packSource.manager.packs.forEach(pack -> pack.activationStatus = ServerPackManager.ActivationStatus.ACTIVE);

                packSource.startReload(new PackReloadConfig.Callbacks() {
                    @Override
                    public @NotNull List<PackReloadConfig.IdAndPath> packsToLoad() {
                        return packSource.manager.packs.stream()
                            .map(pack -> new PackReloadConfig.IdAndPath(pack.id, pack.path))
                            .toList();
                    }
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onFailure(boolean bl) {}
                });
            } catch (Exception ignored) {
            } finally {
                CachingUtils.isStartup = false;
            }
        }
    }
}