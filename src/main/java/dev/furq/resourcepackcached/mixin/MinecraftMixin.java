package dev.furq.resourcepackcached.mixin;

import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "doWorldLoad", at = @At("HEAD"))
    public void beforeWorldLoad(CallbackInfo ci) {
        CachingUtils.popAllLivePacks(((Minecraft) (Object) this).getDownloadedPackSource().manager);
    }
}
