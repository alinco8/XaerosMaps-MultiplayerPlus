package dev.alinco8.xmmp.mixin.client;

import dev.alinco8.xmmp.XMMPClient;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapWriter;
import xaero.map.biome.BlockTintProvider;
import xaero.map.region.MapUpdateFastConfig;
import xaero.map.region.OverlayManager;

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
    private void onWriteChunk(Level world, Registry<Block> blockRegistry, int distance,
        boolean onlyLoad, Registry<Biome> biomeRegistry, OverlayManager overlayManager,
        boolean loadChunks, boolean updateChunks, boolean ignoreHeightmaps, boolean flowers,
        boolean detailedDebug, MutableBlockPos mutableBlockPos3,
        BlockTintProvider blockTintProvider, int caveDepth, int caveStart, int layerToWrite,
        int tileChunkX, int tileChunkZ, int tileChunkLocalX, int tileChunkLocalZ, int chunkX,
        int chunkZ, MapUpdateFastConfig updateConfig, CallbackInfoReturnable<Boolean> cir
    ) {
        if (layerToWrite != Integer.MAX_VALUE) return;

        XMMPClient.onWriteChunk(chunkX, chunkZ);
    }
}
