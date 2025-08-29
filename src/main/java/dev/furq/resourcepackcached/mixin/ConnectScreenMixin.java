package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >1.20.4
import net.minecraft.client.multiplayer.TransferState;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @Inject(method = "connect", at = @At("HEAD"))
    public void onConnect(Minecraft minecraft, ServerAddress serverAddress, ServerData serverData,/*? if >1.20.4 {*/ TransferState transferState, /*?}*/ CallbackInfo ci) {
        if (CachingUtils.cacheServer != null && !CachingUtils.cacheServer.equals(serverData.ip)) CachingUtils.shouldClearPacks = true;
    }
}