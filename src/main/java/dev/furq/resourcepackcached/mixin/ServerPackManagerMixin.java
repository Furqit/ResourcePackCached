package dev.furq.resourcepackcached.mixin;

import com.google.common.hash.HashCode;
import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

@Mixin(ServerPackManager.class)
abstract class ServerPackManagerMixin {
    @Unique
    private final HashMap<UUID, Path> latestPacks = new HashMap<>();
    @Final
    @Shadow
    PackLoadFeedback packLoadFeedback;
    @Unique
    private boolean isNewSequence = true;

    @Shadow
    public abstract void popPack(UUID id);

    @Shadow
    abstract void registerForUpdate();

    @Inject(method = "pushPack", at = @At("HEAD"), cancellable = true)
    public void onPushPack(UUID id, URL url, @Nullable HashCode hashCode, CallbackInfo ci) {
        if (isNewSequence) {
            latestPacks.clear();
            isNewSequence = false;
        }
        if (hashCode != null) {
            Path downloadPath = new File(CachingUtils.GAME_DIR, "downloads/" + id + "/" + hashCode).toPath();
            latestPacks.put(id, downloadPath);

            if (CachingUtils.isCachedResourcePack(id, hashCode)) {
                this.packLoadFeedback.reportUpdate(id, PackLoadFeedback.Update.ACCEPTED);
                this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.APPLIED);
                this.registerForUpdate();
                ci.cancel();
            }
        }
    }

    @Inject(method = "onDownload", at = @At("RETURN"))
    public void afterDownload(CallbackInfo ci) {
        isNewSequence = true;
        if (!latestPacks.isEmpty()) {
            HashMap<UUID, Path> cachedPacks = CachingUtils.readCacheFile();
            cachedPacks.keySet().stream()
                    .filter(uuid -> !latestPacks.containsKey(uuid))
                    .forEach(this::popPack);

            CachingUtils.writeCacheFile(latestPacks);
        }
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
        latestPacks.clear();
        ci.cancel();
    }
}