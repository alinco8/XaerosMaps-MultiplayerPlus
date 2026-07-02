package dev.alinco8.xmmp.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.region.MapPixel;

@Mixin(value = MapPixel.class, remap = false)
public interface MapPixelAccessor {

    @Accessor("light")
    byte getLight();

    @Accessor("glowing")
    boolean getGlowing();
}
