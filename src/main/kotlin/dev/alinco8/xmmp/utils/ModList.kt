package dev.alinco8.xmmp.utils

object ModList {
    fun isModLoaded(modId: String): Boolean {
        //? if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId)
        *///? } else if neoforge {
        return net.neoforged.fml.loading.LoadingModList.get().mods.any { it.modId == modId }
        //? } else if forge {
        /*return net.minecraftforge.fml.loading.LoadingModList.get().mods.any { it.modId == modId }
        *///? }
    }
}
