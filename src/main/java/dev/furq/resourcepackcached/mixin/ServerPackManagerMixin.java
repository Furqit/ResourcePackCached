package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.server.packs.DownloadQueue.BatchResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPackManager.class)
public abstract class ServerPackManagerMixin {

    @Shadow
    @Final
    PackLoadFeedback packLoadFeedback;

    @Inject(method = "pushNewPack", at = @At("HEAD"), cancellable = true)
    public void onPushNewPack(UUID uuid, ServerPackManager.ServerPackData serverPackData, CallbackInfo ci) {
        ServerData data = Minecraft.getInstance().getCurrentServer();
        if (data == null) return;

        if (CachingUtils.shouldClearPacks) {
            Minecraft.getInstance().getDownloadedPackSource().popAll();
            CachingUtils.clearCache();
            CachingUtils.shouldClearPacks = false;
        }

        if (CachingUtils.cacheServer != null && CachingUtils.cacheServer.equals(data.ip) && CachingUtils.isCachedResourcePack(uuid)) {
            this.packLoadFeedback.reportFinalResult(serverPackData.id, PackLoadFeedback.FinalResult.APPLIED);
            ci.cancel();
        }
    }

    @Inject(method = "onDownload", at = @At("RETURN"))
    public void afterDownload(Collection<ServerPackManager.ServerPackData> collection, BatchResult batchResult, CallbackInfo ci) {
        ServerData data = Minecraft.getInstance().getCurrentServer();
        if (data == null) return;

        CachingUtils.cacheServer = data.ip;

        Map<UUID, Path> packs = new HashMap<>();
        for (ServerPackManager.ServerPackData serverPackData : collection) {
            if (serverPackData.path != null) {
                packs.put(serverPackData.id, serverPackData.path);
            }
        }

        if (!packs.isEmpty()) {
            CachingUtils.writeCacheFile(packs);
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
}