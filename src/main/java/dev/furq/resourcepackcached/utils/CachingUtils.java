package dev.furq.resourcepackcached.utils;

//? if fabric {

import net.fabricmc.loader.api.FabricLoader;
//?} elif forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
 *///?} elif neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
 *///?}
import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CachingUtils {

    public static final File CONFIG_DIR = /*? if fabric {*/ FabricLoader.getInstance().getConfigDir().toFile() /*?}*//*? if forge || neoforge {*//* FMLPaths.CONFIGDIR.get().toFile() *//*?}*/;
    public static final File CACHE_FILE = new File(CONFIG_DIR, "rpc-data.json");
    public static final Logger LOGGER = LogManager.getLogger("ResourcePackCached");

    public static boolean isStartup = false;
    public static boolean shouldClearPacks = false;
    public static String cacheServer = null;

    public static void writeCacheFile(Map<UUID, Path> packs) {
        JsonObject root = new JsonObject();
        JsonArray packsArray = new JsonArray();

        packs.forEach((uuid, path) -> {
            if (Files.exists(path)) {
                JsonObject packObj = new JsonObject();
                packObj.addProperty("uuid", uuid.toString());
                packObj.addProperty("path", path.toString());
                packsArray.add(packObj);
            }
        });

        root.addProperty("server", cacheServer == null ? "" : cacheServer);
        root.add("packs", packsArray);

        try {
            Files.writeString(CACHE_FILE.toPath(), root.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to write cache file", e);
        }
    }

    public static Map<UUID, Path> readCacheFile() {
        Map<UUID, Path> packs = new HashMap<>();
        if (!CACHE_FILE.exists()) return packs;

        try {
            String content = Files.readString(CACHE_FILE.toPath(), StandardCharsets.UTF_8);
            if (content.isEmpty()) return packs;

            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            if (root.has("server")) {
                cacheServer = root.get("server").getAsString();
            }

            JsonArray packsArray = root.getAsJsonArray("packs");
            if (packsArray != null) {
                for (JsonElement el : packsArray) {
                    JsonObject packObj = el.getAsJsonObject();
                    UUID uuid = UUID.fromString(packObj.get("uuid").getAsString());
                    Path path = Paths.get(packObj.get("path").getAsString());
                    if (Files.exists(path)) {
                        packs.put(uuid, path);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to read cache file", e);
        }
        return packs;
    }

    public static void clearCache() {
        cacheServer = null;
        if (CACHE_FILE.exists()) {
            try {
                Files.delete(CACHE_FILE.toPath());
            } catch (IOException e) {
                LOGGER.warn("Failed to clear cache file", e);
            }
        }
    }

    public static boolean isCachedResourcePack(UUID uuid) {
        return readCacheFile().containsKey(uuid);
    }
}