package dev.alinco8.xmmp.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import dev.alinco8.xmmp.XMMPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapWriter;

@Mixin(value = MapWriter.class, remap = false)
public class MapWriterMixin {

    @Inject(
        method = "writeChunk",
        at = @At(
            value = "INVOKE",
            target = "Lxaero/map/region/MapTile;setLoaded(Z)V",
            shift = At.Shift.AFTER
        )
    )
    private void onWriteChunk(
        CallbackInfoReturnable<Boolean> cir,
        @Local(argsOnly = true, name = "layerToWrite") int layerToWrite,
        @Local(argsOnly = true, name = "chunkX") int chunkX,
        @Local(argsOnly = true, name = "chunkZ") int chunkZ
    ) {
        if (layerToWrite != Integer.MAX_VALUE) return;

        XMMPClient.onWriteChunk(chunkX, chunkZ);
    }
}
