package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.CompletableFuture;

@Mixin(DownloadedPackSource.class)
public abstract class DownloadedPackSourceMixin {

    @Inject(method = "cleanupAfterDisconnect", at = @At("HEAD"), cancellable = true)
    public void onCleanupAfterDisconnect(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "startReload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<Void> onStartReload(Minecraft instance) {
        return CachingUtils.isStartup ? CompletableFuture.completedFuture(null) : instance.reloadResourcePacks();
    }
}