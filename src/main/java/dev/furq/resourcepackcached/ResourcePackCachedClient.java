package dev.furq.resourcepackcached;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
//? if >1.20.2 {
import net.minecraft.client.resources.server.DownloadedPackSource;
//?} else {
/*import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.PackSource;
*///?}

public class ResourcePackCachedClient implements ClientModInitializer {

    public static final File CACHE_FILE = new File(FabricLoader.getInstance().getGameDir().toFile(), "rpc-data.json");
    public static final Logger LOGGER = LogManager.getLogger(ResourcePackCachedClient.class);
    //? if >1.20.2 {
    public static boolean isProcessing = false;
    public static boolean isJoin = false;
    //?}

    @Override
    public void onInitializeClient() {
        Map<UUID, Path> cachedPacks = CachingUtils.readCacheFile();

        if (!cachedPacks.isEmpty()) {
            try {
                DownloadedPackSource downloadedPackSource = Minecraft.getInstance().getDownloadedPackSource();
                for (Map.Entry<UUID, Path> entry : cachedPacks.entrySet()) {
                    if (Files.exists(entry.getValue())) {
                        //? if >1.20.2 {
                        downloadedPackSource.pushLocalPack(entry.getKey(), entry.getValue());
                        //?} else {
                        /*downloadedPackSource.setServerPack(entry.getValue().toFile(), PackSource.SERVER);
                        *///?}
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
