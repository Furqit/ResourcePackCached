package dev.furq.resourcepackcached.mixin;

import com.google.common.hash.HashCode;
import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ResourcePackHandshakeMixin {

    @Shadow
    public abstract void send(Packet<?> packet);

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"))
    private void spoofResourcePackPackets(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        String hash = packet.hash();
        if (hash != null && !hash.isEmpty()) {
            try {
                if (CachingUtils.isCachedResourcePack(packet.id(), HashCode.fromString(hash))) {
                    this.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
                    this.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
                }
            } catch (Exception ignored) {
                // Ignore invalid hashes etc.
            }
        }
    }
}