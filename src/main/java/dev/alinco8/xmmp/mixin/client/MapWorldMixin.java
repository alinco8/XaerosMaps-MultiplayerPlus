package dev.alinco8.xmmp.mixin.client;

import dev.alinco8.xmmp.XMMPClient;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.world.MapWorld;

@Mixin(value = MapWorld.class, remap = false)
public class MapWorldMixin {

    @Shadow
    private ResourceKey<Level> currentDimensionId;
    @Unique
    @Nullable
    private String xmmp$lastDimension = null;

    @Inject(
        method = "switchToFutureUnsynced",
        at = @At("TAIL")
    )
    private void onXaeroDimensionChanged(CallbackInfo ci) {
        String currentDimension = this.currentDimensionId
            //? if <26 {
            /*.location()
            *///? } else {
            .identifier()
            //? }
            .toString();
        if (currentDimension.equals(xmmp$lastDimension)) return;
        xmmp$lastDimension = currentDimension;

        XMMPClient.onXaeroDimensionChanged(currentDimension);
    }
}
