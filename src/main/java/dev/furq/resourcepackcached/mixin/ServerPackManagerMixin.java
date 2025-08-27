package dev.furq.resourcepackcached.mixin;

import com.google.common.hash.HashCode;
import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Mixin(ServerPackManager.class)
abstract class ServerPackManagerMixin {
    @Unique
    private final HashMap<UUID, Path> latestPacks = new HashMap<>();
    @Shadow
    PackLoadFeedback packLoadFeedback;
    @Unique
    private boolean isNewSequence = true;
    @Shadow
    private ServerPackManager.PackPromptStatus packPromptStatus;

    @Shadow
    public abstract void popPack(UUID id);

    @Shadow
    abstract void registerForUpdate();

    @Inject(method = "pushLocalPack", at = @At("HEAD"))
    private void onPushLocalPack(UUID uUID, Path path, CallbackInfo ci) {
        if (CachingUtils.isCachedResourcePack(uUID, path)) {
            packPromptStatus = ServerPackManager.PackPromptStatus.ALLOWED;
        }
    }

    @Inject(method = "pushPack", at = @At("HEAD"), cancellable = true)
    public void onPushPack(UUID id, URL url, @Nullable HashCode hashCode, CallbackInfo ci) {
        try {
            CachingUtils.isProcessing = true;
            if (isNewSequence) {
                latestPacks.clear();
                isNewSequence = false;
            }
            if (hashCode != null) {
                File downloadPath = new File(CachingUtils.GAME_DIR, "downloads/" + id + "/" + hashCode);
                latestPacks.put(id, downloadPath.toPath());

                if (CachingUtils.isCachedResourcePack(id, downloadPath.toPath())) {
                    this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.APPLIED);
                    CachingUtils.isJoin = false;
                    CachingUtils.isProcessing = false;
                    this.registerForUpdate();
                    ci.cancel();
                }
            }
        } catch (Exception e) {
            CachingUtils.LOGGER.error("Error in pushPack", e);
        } finally {
            CachingUtils.isJoin = false;
        }
    }

    @Inject(method = "cleanupRemovedPacks", at = @At("HEAD"))
    private void onCleanupRemovedPacks(CallbackInfo ci) {
        isNewSequence = true;
        if (!CachingUtils.isProcessing && !CachingUtils.isJoin) {
            if (!latestPacks.isEmpty()) {
                HashMap<UUID, Path> cachedPacks = CachingUtils.readCacheFile();
                List<UUID> packsToRemove = new ArrayList<>();

                for (HashMap.Entry<UUID, Path> pack : cachedPacks.entrySet()) {
                    if (!latestPacks.containsKey(pack.getKey())) {
                        packsToRemove.add(pack.getKey());
                    }
                }

                for (UUID packId : packsToRemove) {
                    this.popPack(packId);
                }

                CachingUtils.writeCacheFile(latestPacks);
                CachingUtils.isJoin = true;
            }
        }
    }

    @Inject(method = "popAll", at = @At("HEAD"), cancellable = true)
    private void onPopAll(CallbackInfo ci) {
        latestPacks.clear();
        CachingUtils.isJoin = true;
        ci.cancel();
    }
}
