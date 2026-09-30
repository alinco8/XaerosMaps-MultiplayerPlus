package dev.alinco8.xmmp.utils

object ModList {
    fun isModLoaded(modId: String): Boolean {
        //? fabric
        //return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId)
        //? neoforge
        return net.neoforged.fml.loading.LoadingModList.get().mods.any { it.modId == modId }
        //? forge
        //return net.minecraftforge.fml.loading.LoadingModList.get().mods.any { it.modId == modId }
    }
}
