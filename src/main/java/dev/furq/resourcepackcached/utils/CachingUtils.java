package dev.furq.resourcepackcached.utils;

import com.google.common.hash.HashCode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.server.ServerPackManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class CachingUtils {

    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<UUID> incomingIds = new HashSet<>();
    public static Path DOWNLOADS_DIR;
    public static Path CACHE_FILE;
    public static boolean isStartup = false;
    public static String currentConnectionHost;
    private static boolean pushed = false;
    private static boolean batchPending = false;

    public static void bootstrap(Path gameDir, Path configDir) {
        DOWNLOADS_DIR = gameDir.resolve("downloads");
        CACHE_FILE = configDir.resolve("rpc-data.json");
        try {
            Files.createDirectories(DOWNLOADS_DIR);
            Files.createDirectories(CACHE_FILE.getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create cache directories", e);
        }
        resetSession();
        isStartup = false;
    }

    public static void resetSession() {
        incomingIds.clear();
        pushed = false;
        batchPending = false;
        currentConnectionHost = null;
    }

    public static void onIncomingPush(UUID id) {
        if (!pushed) {
            incomingIds.clear();
        }
        incomingIds.add(id);
        pushed = true;
    }

    public static void markBatchPending() {
        batchPending = true;
    }

    public static void sweepIncomingPacks(ServerPackManager manager) {
        if (!pushed) {
            return;
        }
        pushed = false;
        popUnpushedPacks(manager);
    }

    public static void configurationFinished(ServerPackManager manager, String host) {
        currentConnectionHost = host;
        boolean pushless = incomingIds.isEmpty();
        if (!pushless || !(host != null && host.equals(jsonHost()))) {
            popUnpushedPacks(manager);
        }
        incomingIds.clear();
        pushed = false;
        if (batchPending) {
            LOGGER.info("Config finished for {} while batch is in flight; snapshot deferred", host);
            return;
        }
        Map<UUID, Path> packs = livePacks(manager);
        LOGGER.info("Config finished for {}: {} pack file(s) live", host, packs.size());
        writeSnapshot(host, packs);
    }

    public static void afterBatch(boolean failed, ServerPackManager manager) {
        batchPending = false;
        if (failed) {
            LOGGER.warn("Download batch failed for {}; previous snapshot kept", currentConnectionHost);
            return;
        }
        Map<UUID, Path> packs = livePacks(manager);
        LOGGER.info("Download batch completed for {}: {} pack file(s) now live", currentConnectionHost, packs.size());
        writeSnapshot(currentConnectionHost, packs);
    }

    public static void snapshotOnReload(ServerPackManager manager) {
        if (isStartup || currentConnectionHost == null) {
            return;
        }
        Map<UUID, Path> packs = livePacks(manager);
        LOGGER.info("Reload checkpoint for {}: {} pack file(s) live", currentConnectionHost, packs.size());
        writeSnapshot(currentConnectionHost, packs);
    }

    public static void popAllLivePacks(ServerPackManager manager) {
        if (manager == null) {
            return;
        }
        for (ServerPackManager.ServerPackData pack : List.copyOf(manager.packs)) {
            if (!pack.isRemoved()) {
                manager.popPack(pack.id);
            }
        }
    }

    private static void popUnpushedPacks(ServerPackManager manager) {
        if (manager == null) {
            return;
        }
        for (ServerPackManager.ServerPackData pack : List.copyOf(manager.packs)) {
            if (!pack.isRemoved() && pack.path != null && !incomingIds.contains(pack.id)) {
                manager.popPack(pack.id);
            }
        }
    }

    private static Map<UUID, Path> livePacks(ServerPackManager manager) {
        Map<UUID, Path> packs = new HashMap<>();
        if (manager != null) {
            for (ServerPackManager.ServerPackData pack : List.copyOf(manager.packs)) {
                if (!pack.isRemoved() && pack.path != null) {
                    packs.put(pack.id, pack.path);
                }
            }
        }
        return packs;
    }

    public static Map<UUID, Path> getCachedPacks() {
        Map<UUID, Path> packs = new HashMap<>();
        JsonObject root = readJson();
        if (root == null) {
            return packs;
        }
        JsonArray packsArray = root.getAsJsonArray("packs");
        if (packsArray != null) {
            for (int i = 0; i < packsArray.size(); i++) {
                try {
                    JsonObject packObj = packsArray.get(i).getAsJsonObject();
                    UUID uuid = UUID.fromString(packObj.get("uuid").getAsString());
                    Path path = Path.of(packObj.get("path").getAsString());
                    if (Files.exists(path)) {
                        packs.put(uuid, path);
                    }
                } catch (RuntimeException e) {
                    LOGGER.warn("Skipping malformed cache entry #{}", i, e);
                }
            }
        }
        return packs;
    }

    public static void writeCacheFile(Map<UUID, Path> packs) {
        writeSnapshot(null, packs);
    }

    private static void writeSnapshot(String host, Map<UUID, Path> packs) {
        Map<UUID, Path> valid = new HashMap<>();
        for (Map.Entry<UUID, Path> entry : packs.entrySet()) {
            if (Files.isRegularFile(entry.getValue())) {
                valid.put(entry.getKey(), entry.getValue());
            }
        }

        JsonObject root = createRoot(host, valid);

        try {
            Path temp = CACHE_FILE.resolveSibling(CACHE_FILE.getFileName().toString() + ".tmp");
            Files.writeString(temp, root.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(temp, CACHE_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, CACHE_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write cache file", e);
        }
    }

    private static JsonObject createRoot(String host, Map<UUID, Path> valid) {
        JsonObject root = new JsonObject();
        if (host != null) {
            root.addProperty("host", host);
        }
        JsonArray packsArray = new JsonArray();
        for (Map.Entry<UUID, Path> entry : valid.entrySet()) {
            JsonObject packObj = new JsonObject();
            packObj.addProperty("uuid", entry.getKey().toString());
            packObj.addProperty("path", entry.getValue().toString());
            packsArray.add(packObj);
        }
        root.add("packs", packsArray);
        return root;
    }

    public static Map<UUID, Path> verifyCachedFiles() {
        Map<UUID, Path> packs = getCachedPacks();
        boolean changed = false;
        for (Map.Entry<UUID, Path> entry : List.copyOf(packs.entrySet())) {
            boolean valid;
            try {
                valid = hashFile(entry.getValue()).equals(HashCode.fromString(entry.getValue().getFileName().toString()));
            } catch (IllegalArgumentException e) {
                valid = false;
            } catch (IOException e) {
                LOGGER.warn("Failed to read cached pack file {}", entry.getValue(), e);
                valid = false;
            }
            if (valid) {
                continue;
            }
            LOGGER.warn("Cached pack file {} failed hash verification, ignoring it", entry.getValue());
            try {
                Files.deleteIfExists(entry.getValue());
            } catch (IOException e) {
                LOGGER.warn("Failed to delete invalid cached pack file {}", entry.getValue(), e);
            }
            packs.remove(entry.getKey());
            changed = true;
        }
        if (changed) {
            writeSnapshot(jsonHost(), packs);
        }
        return packs;
    }

    public static boolean isCachedResourcePack(UUID uuid, HashCode hashCode) {
        return Files.isRegularFile(DOWNLOADS_DIR.resolve(uuid.toString()).resolve(hashCode.toString()));
    }

    private static String jsonHost() {
        JsonObject root = readJson();
        return root != null && root.has("host") && root.get("host").isJsonPrimitive()
                ? root.get("host").getAsString()
                : null;
    }

    private static JsonObject readJson() {
        if (!Files.exists(CACHE_FILE)) {
            return null;
        }
        try {
            String content = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            if (content.isEmpty()) {
                return null;
            }
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to read cache file, treating it as empty", e);
            return null;
        }
    }

    private static HashCode hashFile(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HashCode.fromBytes(digest.digest());
    }
}