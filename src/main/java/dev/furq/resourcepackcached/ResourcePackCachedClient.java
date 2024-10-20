package dev.furq.resourcepackcached;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//? if >1.20.2 {
import net.minecraft.client.resources.server.DownloadedPackSource;
import java.util.UUID;
//?} else {
/*import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.PackSource;
*///?}

public class ResourcePackCachedClient implements ClientModInitializer {

    public static final File CACHE_FILE = new File(FabricLoader.getInstance().getGameDir().toFile(), "rpc-data.json");
    private static final Logger LOGGER = LogManager.getLogger(ResourcePackCachedClient.class);
    public static File cacheResourcePackFile = null;

    public static void setLatestServerResourcePack(File file) {
        if (file != null) {
            writeCacheFile(file.getPath());
            cacheResourcePackFile = file;
        }
    }

    private static void writeCacheFile(String filePath) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("file", filePath);

        try {
            FileUtils.writeStringToFile(CACHE_FILE, jsonObject.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static JsonObject readCacheFile() {
        if (CACHE_FILE.exists()) {
            try {
                return JsonParser.parseString(FileUtils.readFileToString(CACHE_FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    //? if >1.20.2 {
    public static UUID extractUUID(Path resourcePack) {
        if (resourcePack == null || resourcePack.getParent() == null) return null;

        String folderName = resourcePack.getParent().getFileName().toString();
        try {
            return UUID.fromString(folderName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isCachedResourcePack(Path path) {
        JsonObject jsonObject = ResourcePackCachedClient.readCacheFile();
        if (jsonObject != null) {
            String cachedPath = jsonObject.get("file").getAsString();
            return path.toString().equals(cachedPath);
        }
        return false;
    }
    //?}

    @Override
    public void onInitializeClient() {
        JsonObject jsonObject = readCacheFile();

        if (jsonObject != null) {
            String filePath = jsonObject.get("file").getAsString();
            Path resourcePack = Paths.get(filePath);
            //? if >1.20.2 {
            UUID uuid = extractUUID(resourcePack);
            //?}
            if (/*? if >1.20.2 {*/uuid != null &&/*?}*/ Files.exists(resourcePack)) {
                cacheResourcePackFile = resourcePack.toFile();
                try {
                    DownloadedPackSource downloadedPackSource = Minecraft.getInstance().getDownloadedPackSource();
                    //? if >1.20.2 {
                    downloadedPackSource.pushLocalPack(uuid, resourcePack);
                    //?} else {
                    /*downloadedPackSource.setServerPack(new File(filePath), PackSource.SERVER);
                     *///?}
                } catch (Exception e) {
                    LOGGER.error("Error pushing local pack", e);
                }
            }
        }
    }
}