package dev.furq.resourcepackcached.utils;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} elif forge {
/*import net.minecraftforge.fml.loading.FMLPaths;
 *///?} elif neoforge {
/*import net.neoforged.fml.loading.FMLPaths;
 *///?}
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

    public static final File GAME_DIR = /*? if fabric {*/FabricLoader.getInstance().getGameDir().toFile()/*?}*//*? if forge {*//*FMLPaths.GAMEDIR.get().toFile()*//*?}*//*? if neoforge {*//*FMLPaths.GAMEDIR.get().toFile()*//*?}*/;
    public static final File CACHE_FILE = new File(GAME_DIR, "rpc-data.json");
    public static final Logger LOGGER = LogManager.getLogger(CachingUtils.class);
    //? if >1.20.2 {
    public static boolean isProcessing = false;
    public static boolean isJoin = false;
    //?}

    public static void writeCacheFile(HashMap<UUID, Path> packs) {
        try {
            JsonObject jsonObject = new JsonObject();
            JsonArray packsArray = new JsonArray();

            for (HashMap.Entry<UUID, Path> entry : packs.entrySet()) {
                if (Files.exists(entry.getValue())) {
                    JsonObject packObject = new JsonObject();
                    packObject.addProperty("uuid", entry.getKey().toString());
                    packObject.addProperty("path", entry.getValue().toString());
                    packsArray.add(packObject);
                }
            }

            jsonObject.add("packs", packsArray);
            FileUtils.writeStringToFile(CACHE_FILE, jsonObject.toString(), StandardCharsets.UTF_8);
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
                    JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();
                    JsonArray packsArray = jsonObject.getAsJsonArray("packs");
                    if (packsArray != null) {
                        for (int i = 0; i < packsArray.size(); i++) {
                            JsonObject packObject = packsArray.get(i).getAsJsonObject();
                            UUID uuid = UUID.fromString(packObject.get("uuid").getAsString());
                            Path path = Paths.get(packObject.get("path").getAsString());
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

    //? if >1.20.2 {
    public static boolean isCachedResourcePack(UUID uuid, Path path) {
        HashMap<UUID, Path> cachedPacks = readCacheFile();
        return cachedPacks.containsKey(uuid) && cachedPacks.get(uuid).equals(path);
    }
    //?}
}