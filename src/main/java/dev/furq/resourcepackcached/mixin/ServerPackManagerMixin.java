package dev.furq.resourcepackcached.mixin;

import com.google.common.hash.HashCode;
import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.server.packs.DownloadQueue;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

@Mixin(ServerPackManager.class)
abstract class ServerPackManagerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    public void onManagerTick(CallbackInfo ci) {
        CachingUtils.sweepIncomingPacks((ServerPackManager) (Object) this);
    }

    @Inject(method = "pushPack", at = @At("HEAD"), cancellable = true)
    public void onPushPack(UUID id, URL url, @Nullable HashCode hash, CallbackInfo ci) {
        CachingUtils.onIncomingPush(id);
        if (hash != null) {
            Path downloadPath = CachingUtils.DOWNLOADS_DIR.resolve(id.toString()).resolve(hash.toString());
            if (CachingUtils.isCachedResourcePack(id, hash)) {
                CachingUtils.LOGGER.info("Serving pack {} from cached file {}", id, hash);
                ServerPackManager manager = (ServerPackManager) (Object) this;
                if (hasLivePack(id)) {
                    manager.packLoadFeedback.reportUpdate(id, PackLoadFeedback.Update.ACCEPTED);
                    manager.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.APPLIED);
                    manager.registerForUpdate();
                } else {
                    manager.packLoadFeedback.reportUpdate(id, PackLoadFeedback.Update.ACCEPTED);
                    manager.packLoadFeedback.reportUpdate(id, PackLoadFeedback.Update.DOWNLOADED);
                    manager.pushLocalPack(id, downloadPath);
                }
                ci.cancel();
            }
        }

        if (!ci.isCancelled()) {
            CachingUtils.markBatchPending();
        }
    }

    @Unique
    private boolean hasLivePack(UUID id) {
        for (ServerPackManager.ServerPackData pack : ((ServerPackManager) (Object) this).packs) {
            if (pack.id.equals(id) && !pack.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "onDownload", at = @At("RETURN"))
    public void afterDownload(Collection<ServerPackManager.ServerPackData> data, DownloadQueue.BatchResult result, CallbackInfo ci) {
        CachingUtils.afterBatch(!result.failed().isEmpty(), (ServerPackManager) (Object) this);
    }

    @Inject(method = "registerForUpdate", at = @At("HEAD"), cancellable = true)
    public void onRegisterForUpdate(CallbackInfo ci) {
        if (CachingUtils.isStartup) ci.cancel();
    }

    @Inject(method = "triggerReloadIfNeeded", at = @At("HEAD"), cancellable = true)
    public void onTriggerReloadIfNeeded(CallbackInfo ci) {
        if (CachingUtils.isStartup) ci.cancel();
    }

    @Inject(method = "popAll", at = @At("HEAD"), cancellable = true)
    private void onPopAll(CallbackInfo ci) {
        ci.cancel();
    }
}