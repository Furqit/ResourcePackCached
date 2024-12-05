package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
//? if >1.20.2 {
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.client.resources.server.PackReloadConfig;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.google.common.hash.HashCode;

import java.net.URL;
//?} elif <=1.20.2 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Final;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
*///?}
//? if =1.20.2 {
/*import net.minecraft.SharedConstants;
 *///?}

//? if <=1.20.2 {
/*@Mixin(DownloadedPackSource.class)
abstract class DownloadedPackSourceMixin {

    @Shadow
    @Final
    private static Component SERVER_NAME;
    @Shadow
    @Final
    private static Logger LOGGER;
    @Unique
    private static boolean isInitializing = true;
    @Shadow
    @Nullable
    private Pack serverPack;
    @Shadow
    @Nullable
    private CompletableFuture<?> currentDownload;
    @Shadow
    @Final
    private ReentrantLock downloadLock;

    @Inject(method = "clearServerPack", at = @At("HEAD"), cancellable = true)
    public void onClearServerPack(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        this.downloadLock.lock();

        CompletableFuture<Void> completablefuture;
        try {
            if (this.currentDownload != null) {
                this.currentDownload.cancel(true);
            }

            this.currentDownload = null;
            completablefuture = CompletableFuture.completedFuture(null);
        } finally {
            this.downloadLock.unlock();
        }

        cir.setReturnValue(completablefuture);
    }

    @Inject(method = "setServerPack", at = @At("HEAD"), cancellable = true)
    public void onSetServerPack(File file, PackSource packSource, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        //? if <1.20.2 {
        Pack.ResourcesSupplier resourcesSupplier = (string) ->
                new FilePackResources(string, file, false);
        Pack.Info info = Pack.readPackInfo("server", resourcesSupplier);
        //?} else {
        /^Pack.ResourcesSupplier resourcesSupplier = new FilePackResources.FileResourcesSupplier(file, false);
        int i = SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES);
        Pack.Info info = Pack.readPackInfo("server", resourcesSupplier, i);
        ^///?}
        if (info == null) {
            LOGGER.error("Invalid pack metadata at {}", file);
            cir.setReturnValue(CompletableFuture.failedFuture(new IllegalArgumentException("Invalid pack metadata at " + file)));
        } else {
            LOGGER.info("Applying server pack {}", file);

            Pack newServerPack = Pack.create("server", SERVER_NAME, true, resourcesSupplier, info, /^? if <1.20.2 {^/PackType.CLIENT_RESOURCES,/^?}^/ Pack.Position.TOP, true, packSource);
            Map<UUID, Path> cachedPacks = CachingUtils.readCacheFile();
            final UUID SUBSTITUTE_UUID = UUID.fromString("b700a6a9-58e1-4e5b-a995-ead5edc8f72a");
            Path resourcePack = cachedPacks.get(SUBSTITUTE_UUID);

            CompletableFuture<Void> returnValue = null;

            if (resourcePack == null || isInitializing) {
                cachedPacks.put(SUBSTITUTE_UUID, file.toPath());
                CachingUtils.writeCacheFile(cachedPacks);
                this.serverPack = newServerPack;
                returnValue = Minecraft.getInstance().delayTextureReload();
            } else if (this.serverPack != null || !file.equals(resourcePack.toFile())) {
                this.serverPack = newServerPack;
                if (!file.equals(resourcePack.toFile())) {
                    cachedPacks.put(SUBSTITUTE_UUID, file.toPath());
                    CachingUtils.writeCacheFile(cachedPacks);
                    returnValue = Minecraft.getInstance().delayTextureReload();
                }
            }

            if (returnValue == null) {
                returnValue = CompletableFuture.completedFuture(null);
            }

            isInitializing = false;
            cir.setReturnValue(returnValue);
        }
    }
}
*///?} else {
@Mixin(DownloadedPackSource.class)
abstract class DownloadedPackSourceMixin {

    @Inject(method = "loadRequestedPacks", at = @At("RETURN"))
    private void loadRequestedPacks(List<PackReloadConfig.IdAndPath> list, CallbackInfoReturnable<List<Pack>> cir) {
        CachingUtils.isProcessing = false;
    }
}

@Mixin(ServerPackManager.class)
abstract class ServerPackManagerMixin {

    @Unique
    private final Map<UUID, Path> latestPacks = new HashMap<>();
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
                Map<UUID, Path> cachedPacks = CachingUtils.readCacheFile();
                List<UUID> packsToRemove = new ArrayList<>();

                for (Map.Entry<UUID, Path> pack : cachedPacks.entrySet()) {
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
//?}
