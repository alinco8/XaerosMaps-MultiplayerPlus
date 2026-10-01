package dev.alinco8.xmmp.mixin.compat.xaerominimap;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.alinco8.xmmp.client.XMMPClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.common.mods.SupportXaeroWorldmap;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public class SupportXaeroWorldmapMixin {

    @ModifyReturnValue(method = "getCaveModeType", at = @At("RETURN"))
    private int xmmp$forceFullCaveMode(int original) {
        var session = XMMPClient.INSTANCE.getSession();
        if (session == null) return original;
        if (!session.getServerConfig().getSyncCaves()) return original;

        return original == 1 ? 2 : original;
    }
}
