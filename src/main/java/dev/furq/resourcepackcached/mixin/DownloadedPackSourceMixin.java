package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.CompletableFuture;

@Mixin(DownloadedPackSource.class)
public class DownloadedPackSourceMixin {

    @Redirect(method = "startReload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;"))
    public CompletableFuture<Void> onStartReload(Minecraft instance) {
        return CachingUtils.isStartup ? CompletableFuture.completedFuture(null) : instance.reloadResourcePacks();
    }

    @Inject(method = "configureForServerControl", at = @At("HEAD"))
    private void onConfigureForServerControl(Connection connection, ServerPackManager.PackPromptStatus packPromptStatus, CallbackInfo ci) {
        CachingUtils.onNewConnection();
    }

    @Inject(method = "configureForLocalWorld", at = @At("HEAD"))
    private void onConfigureForLocalWorld(CallbackInfo ci) {
        CachingUtils.onNewConnection();
    }
}