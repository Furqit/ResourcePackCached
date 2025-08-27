package dev.furq.resourcepackcached;

//? if fabric {
import it.unimi.dsi.fastutil.Hash;
import net.fabricmc.api.ClientModInitializer;
//?} elif forge {
/*import net.minecraftforge.fml.common.Mod;
*///?} elif neoforge {
/*import net.neoforged.fml.common.Mod;
*///?}
import dev.furq.resourcepackcached.utils.CachingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

//? if forge || neoforge {
/*@Mod("rpc")*/
public class ResourcePackCached /*? if fabric {*/ implements ClientModInitializer/*?}*/ {

    //? if fabric {
    @Override
    public void onInitializeClient() {
        //?} else {
        /*public ResourcePackCached() {
         *///?}
        HashMap<UUID, Path> cachedPacks = CachingUtils.readCacheFile();
        if (!cachedPacks.isEmpty()) {
            try {
                DownloadedPackSource downloadedPackSource = Minecraft.getInstance().getDownloadedPackSource();
                for (HashMap.Entry<UUID, Path> entry : cachedPacks.entrySet()) {
                    if (Files.exists(entry.getValue())) {
                        downloadedPackSource.pushLocalPack(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}