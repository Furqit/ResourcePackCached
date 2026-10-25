package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public class ClientConfigurationListenerMixin {

    @Inject(method = "handleConfigurationFinished", at = @At("RETURN"))
    public void afterConfigurationFinished(CallbackInfo ci) {
        ServerData serverData = ((ClientCommonPacketListenerImpl) (Object) this).serverData;
        CachingUtils.configurationFinished(Minecraft.getInstance().getDownloadedPackSource().manager, serverData == null ? null : serverData.ip);
    }
}
