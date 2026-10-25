package dev.furq.resourcepackcached;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.client.resources.server.ServerPackManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ResourcePackCached {

    static void boot(Path gameDir, Path configDir) {
        CachingUtils.bootstrap(gameDir, configDir);
        Map<UUID, Path> cachedPacks = CachingUtils.verifyCachedFiles();
        if (!cachedPacks.isEmpty()) {
            try {
                CachingUtils.isStartup = true;
                DownloadedPackSource packSource = Minecraft.getInstance().getDownloadedPackSource();
                packSource.manager.packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;

                cachedPacks.forEach(packSource::pushLocalPack);

                for (ServerPackManager.ServerPackData pack : packSource.manager.packs) {
                    if (cachedPacks.containsKey(pack.id)) {
                        pack.activationStatus = ServerPackManager.ActivationStatus.ACTIVE;
                    }
                }

                packSource.startReload(new PackReloadConfig.Callbacks() {
                    @Override
                    public @NotNull List<PackReloadConfig.IdAndPath> packsToLoad() {
                        return packSource.manager.packs.stream()
                                .map(pack -> new PackReloadConfig.IdAndPath(pack.id, Objects.requireNonNull(pack.path)))
                                .toList();
                    }

                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onFailure(boolean bl) {
                        CachingUtils.LOGGER.warn("Failed to stage cached resource packs (recovery: {}); servers will re-send them on join", bl);
                    }
                });
            } catch (Exception e) {
                CachingUtils.LOGGER.error("Failed to stage cached resource packs", e);
            } finally {
                CachingUtils.isStartup = false;
            }
        }
    }
}
