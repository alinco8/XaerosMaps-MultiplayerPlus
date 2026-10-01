package dev.alinco8.xmmp.mixin.compat.xaeroworldmap;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.alinco8.xmmp.client.XMMPClient;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.map.MapWriter;
import xaero.map.region.MapTile;

@Mixin(value = MapWriter.class, remap = false)
public class MapWriterMixin {

    @WrapOperation(
        method = "writeChunk",
        at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTile;setLoaded(Z)V")
    )
    private void xmmp$onLoaded(
        MapTile tile,
        boolean loaded,
        Operation<Void> orig,
        @Local(argsOnly = true) Level world,
        @Local(argsOnly = true, ordinal = 3) int layerToWrite,
        @Local(argsOnly = true, ordinal = 8) int chunkX,
        @Local(argsOnly = true, ordinal = 9) int chunkZ
    ) {
        orig.call(tile, loaded);
        if (layerToWrite != Integer.MAX_VALUE && layerToWrite != Integer.MIN_VALUE) return;

        XMMPClient.onTileWritten(world.dimension(), layerToWrite, chunkX, chunkZ, tile);
    }
}
