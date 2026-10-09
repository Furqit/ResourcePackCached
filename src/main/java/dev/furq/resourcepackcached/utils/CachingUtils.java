package dev.furq.resourcepackcached.utils;

import com.google.common.hash.HashCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
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
import java.util.Locale;
import java.util.UUID;

public class CachingUtils {

    public static final File GAME_DIR = FabricLoader.getInstance().getGameDir().toFile();
    public static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
    public static final File CACHE_FILE = new File(CONFIG_DIR, "rpc-data.json");
    public static final Logger LOGGER = LogManager.getLogger("ResourcePackCached");
    public static boolean isStartup = false;

    public static volatile boolean receivedPacksThisConnection = false;
    public static volatile boolean forceUnload = false;

    public record CacheData(String server, HashMap<UUID, Path> packs) {
    }

    public static String normalizeServerAddress(String ip) {
        if (ip == null) return null;
        String trimmed = ip.trim();
        if (trimmed.isEmpty()) return null;
        try {
            ServerAddress parsed = ServerAddress.parseString(trimmed);
            String host = parsed.getHost();
            if (host == null || host.isEmpty()) return trimmed.toLowerCase(Locale.ROOT);
            return host.toLowerCase(Locale.ROOT) + ":" + parsed.getPort();
        } catch (Exception e) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
    }

    public static String getCurrentServerAddress() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                ServerData data = mc.getCurrentServer();
                if (data != null && data.ip != null) {
                    String normalized = normalizeServerAddress(data.ip);
                    if (normalized != null) return normalized;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static CacheData readCacheData() {
        HashMap<UUID, Path> packs = new HashMap<>();
        String server = null;
        if (CACHE_FILE.exists()) {
            try {
                String content = FileUtils.readFileToString(CACHE_FILE, StandardCharsets.UTF_8);
                if (!content.isEmpty()) {
                    JsonObject root = JsonParser.parseString(content).getAsJsonObject();
                    if (root.has("server") && !root.get("server").isJsonNull()) {
                        try {
                            String raw = root.get("server").getAsString();
                            if (raw != null && !raw.isEmpty()) server = raw;
                        } catch (Exception ignored) {
                        }
                    }
                    JsonArray packsArray = root.getAsJsonArray("packs");
                    if (packsArray != null) {
                        for (int i = 0; i < packsArray.size(); i++) {
                            try {
                                JsonObject packObj = packsArray.get(i).getAsJsonObject();
                                UUID uuid = UUID.fromString(packObj.get("uuid").getAsString());
                                Path path = Paths.get(packObj.get("path").getAsString());
                                if (Files.exists(path)) {
                                    packs.put(uuid, path);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to read cache file", e);
            }
        }
        return new CacheData(server, packs);
    }

    public static void writeCacheFile(HashMap<UUID, Path> packs) {
        writeCacheFile(packs, getCurrentServerAddress());
    }

    public static void writeCacheFile(HashMap<UUID, Path> packs, String serverAddress) {
        try {
            String server = serverAddress;
            if (server == null) {
                server = readCacheData().server();
            }
            JsonObject root = new JsonObject();
            if (server != null) {
                root.addProperty("server", server);
            }
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

    public static void updateCachedServer() {
        try {
            CacheData data = readCacheData();
            String current = getCurrentServerAddress();
            if (current == null || current.equals(data.server())) return;
            if (data.packs().isEmpty()) return;
            writeCacheFile(data.packs(), current);
        } catch (Exception e) {
            LOGGER.error("Failed to update cached server address", e);
        }
    }

    public static HashMap<UUID, Path> readCacheFile() {
        return readCacheData().packs();
    }

    public static boolean isCachedResourcePack(UUID uuid, HashCode hashCode) {
        Path cachedPath = readCacheFile().get(uuid);
        return cachedPath != null && cachedPath.getFileName().toString().equals(hashCode.toString());
    }

    public static void clearCacheFile() {
        try {
            if (CACHE_FILE.exists() && !CACHE_FILE.delete()) {
                LOGGER.warn("Failed to delete cache file {}", CACHE_FILE);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to clear cache file", e);
        }
    }

    public static void unloadAllPacks() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            if (!mc.isSameThread()) {
                mc.execute(CachingUtils::unloadAllPacks);
                return;
            }
            DownloadedPackSource source = mc.getDownloadedPackSource();
            if (source == null) return;
            forceUnload = true;
            try {
                source.popAll();
            } finally {
                forceUnload = false;
            }
        } catch (Exception e) {
            forceUnload = false;
            LOGGER.error("Failed to unload server packs", e);
        }
    }

    public static void resetCachedPacks() {
        clearCacheFile();
        unloadAllPacks();
        LOGGER.info("ResourcePackCached: cleared cached resource packs");
    }

    public static void onNewConnection() {
        receivedPacksThisConnection = false;
    }

    public static void onGameJoin() {
        if (receivedPacksThisConnection) return;
        if (readCacheData().packs().isEmpty()) return;
        LOGGER.info("ResourcePackCached: no packs received on join, unloading stale packs");
        clearCacheFile();
        unloadAllPacks();
    }
}