package dev.furq.resourcepackcached.utils;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} elif neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
 *///?}
import com.google.common.hash.HashCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.UUID;

public class CachingUtils {

    public static final File GAME_DIR = /*? if fabric {*/ FabricLoader.getInstance().getGameDir().toFile() /*?}*//*? if neoforge {*//* FMLPaths.GAMEDIR.get().toFile() *//*?}*/;
    public static final File CONFIG_DIR = /*? if fabric {*/ FabricLoader.getInstance().getConfigDir().toFile() /*?}*//*? if neoforge {*//* FMLPaths.CONFIGDIR.get().toFile() *//*?}*/;
    public static final File CACHE_FILE = new File(CONFIG_DIR, "rpc-data.json");
    public static final Logger LOGGER = LogManager.getLogger("ResourcePackCached");
    public static boolean isStartup = false;

    public static void writeCacheFile(HashMap<UUID, Path> packs) {
        try {
            JsonObject root = new JsonObject();
            JsonArray packsArray = new JsonArray();

            for (HashMap.Entry<UUID, Path> entry : packs.entrySet()) {
                if (Files.exists(entry.getValue())) {
                    JsonObject packObj = new JsonObject();
                    packObj.addProperty("uuid", entry.getKey().toString());
                    packObj.addProperty("path", entry.getValue().toString());
                    packsArray.add(packObj);
                }
            }

            root.add("packs", packsArray);
            FileUtils.writeStringToFile(CACHE_FILE, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Failed to write cache file", e);
        }
    }

    public static HashMap<UUID, Path> readCacheFile() {
        HashMap<UUID, Path> packs = new HashMap<>();
        if (CACHE_FILE.exists()) {
            try {
                String content = FileUtils.readFileToString(CACHE_FILE, StandardCharsets.UTF_8);
                if (!content.isEmpty()) {
                    JsonObject root = JsonParser.parseString(content).getAsJsonObject();
                    JsonArray packsArray = root.getAsJsonArray("packs");
                    if (packsArray != null) {
                        for (int i = 0; i < packsArray.size(); i++) {
                            JsonObject packObj = packsArray.get(i).getAsJsonObject();
                            UUID uuid = UUID.fromString(packObj.get("uuid").getAsString());
                            Path path = Paths.get(packObj.get("path").getAsString());
                            if (Files.exists(path)) {
                                packs.put(uuid, path);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read cache file", e);
            }
        }
        return packs;
    }

    public static boolean isCachedResourcePack(UUID uuid, HashCode hashCode) {
        Path cachedPath = readCacheFile().get(uuid);
        return cachedPath != null && cachedPath.getFileName().toString().equals(hashCode.toString());
    }
}