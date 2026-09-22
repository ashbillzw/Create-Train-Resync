package com.ashbill.trainresync.mixin.mcef;

import com.cinemamod.mcef.MCEFDownloader;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(value = MCEFDownloader.class, remap = false)
public abstract class MCEFDownloaderMixin {
    @Inject(method = "downloadJavaCefChecksum()Z", at = @At("HEAD"), cancellable = true)
    private void trainresync$useLocalRuntime(CallbackInfoReturnable<Boolean> callback) throws IOException {
        // MCEF has already selected the platform and set its original library paths.
        Path platform = Path.of(System.getProperty("jcef.path")).getFileName();
        Path libraries = Path.of(System.getenv("LOCALAPPDATA"), "AshBill", "mcef-libraries")
                .toAbsolutePath();
        Path runtime = libraries.resolve(platform);

        if (!Files.exists(runtime)) {
            Path source = Minecraft.getInstance().gameDirectory.toPath()
                    .resolve("AshBill").resolve("免下载MCEF").resolve("mcef-libraries").resolve(platform);
            FileUtils.copyDirectory(source.toFile(), runtime.toFile());
        }

        System.setProperty("mcef.libraries.path", libraries.toString());
        System.setProperty("jcef.path", runtime.toString());
        // Upstream treats true as "no download needed", bypassing checksum and archive requests.
        callback.setReturnValue(true);
    }
}
