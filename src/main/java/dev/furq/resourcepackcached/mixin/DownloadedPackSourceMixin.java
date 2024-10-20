package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.ResourcePackCachedClient;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.io.File;
import net.minecraft.server.packs.repository.PackSource;
//? if >1.20.2 {
import com.google.common.hash.HashCode;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackLoadFeedback;
import net.minecraft.client.resources.server.ServerPackManager;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
//?} elif <=1.20.2 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DownloadedPackSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;
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
public abstract class DownloadedPackSourceMixin {

    @Shadow
    @Final
    private static Component SERVER_NAME;
    @Shadow
    @Final
    private static Logger LOGGER;
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
            cir.setReturnValue(CompletableFuture.failedFuture(new IllegalArgumentException("Invalid pack metadata at " + file)));
        } else {
            LOGGER.info("Applying server pack {}", file);

            Pack newServerPack = Pack.create("server", SERVER_NAME, true, resourcesSupplier, info, /^? if <1.20.2 {^/PackType.CLIENT_RESOURCES,/^?}^/ Pack.Position.TOP, true, packSource);
            CompletableFuture<Void> returnValue = null;
            if (this.serverPack == null || !file.equals(ResourcePackCachedClient.cacheResourcePackFile)) {
                this.serverPack = newServerPack;
                if (!file.equals(ResourcePackCachedClient.cacheResourcePackFile)) {
                    ResourcePackCachedClient.setLatestServerResourcePack(file);
                    returnValue = Minecraft.getInstance().delayTextureReload();
                }
            }

            if (returnValue == null) {
                returnValue = new CompletableFuture<>();
                try {
                    returnValue.complete(null);
                } catch (Exception ignored) {
                }
            }

            cir.setReturnValue(returnValue);
        }
    }
}
*///?} else {
@Mixin(DownloadedPackSource.class)
public abstract class DownloadedPackSourceMixin {

    @Shadow
    ServerPackManager manager;

    @Shadow
    private PackSource packType;

    @Inject(method = "pushLocalPack", at = @At("HEAD"))
    private void onPushLocalPack(UUID uUID, Path path, CallbackInfo ci) {
        if (ResourcePackCachedClient.isCachedResourcePack(path)) {
            this.packType = PackSource.SERVER;
            this.manager.allowServerPacks();
        }
    }
}

@Mixin(ServerPackManager.class)
abstract class ServerPackManagerMixin {

    @Shadow PackLoadFeedback packLoadFeedback;

    @Shadow
    public abstract void popPack(UUID id);

    @Inject(method = "pushPack", at = @At("HEAD"), cancellable = true)
    public void addPushPack(UUID id, URL url, HashCode hashCode, CallbackInfo ci) {
        JsonObject jsonObject = ResourcePackCachedClient.readCacheFile();

        if (jsonObject != null) {
            String path = jsonObject.get("file").getAsString();
            if (path.contains(hashCode.toString())) {
                this.packLoadFeedback.reportFinalResult(id, PackLoadFeedback.FinalResult.APPLIED);
                ci.cancel();
            } else {
                UUID cachedUUID = ResourcePackCachedClient.extractUUID(Paths.get(path));
                if (cachedUUID != null) {
                    this.popPack(cachedUUID);
                }
            }
        }
        File downloadPath = new File(FabricLoader.getInstance().getGameDir().toFile().getPath(), "downloads/" + id + "/" + hashCode.toString());
        ResourcePackCachedClient.setLatestServerResourcePack(downloadPath);
    }

    @Inject(method = "popAll", at = @At("HEAD"), cancellable = true)
    private void onCleanupAfterDisconnect(CallbackInfo ci) {
        ci.cancel();
    }
}
//?}