package dev.alinco8.xmmp.mixin.client;

import dev.alinco8.xmmp.XMMPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.file.MapSaveLoad;

@Mixin(MapSaveLoad.class)
public class MapSaveLoadMixin {

    @Unique
    private boolean xmmp$lastRegionDetectionComplete;

    @Inject(
        method = "setRegionDetectionComplete",
        at = @At("HEAD")
    )
    private void setRegionDetectionComplete(boolean regionDetectionComplete, CallbackInfo ci) {
        if (xmmp$lastRegionDetectionComplete == regionDetectionComplete) return;
        xmmp$lastRegionDetectionComplete = regionDetectionComplete;

        XMMPClient.onXaeroStateChanged(regionDetectionComplete);
    }
}
