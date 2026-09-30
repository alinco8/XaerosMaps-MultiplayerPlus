package dev.alinco8.xmmp.utils

import java.nio.file.Path

object ModPaths {
    fun configDir(): Path {
        //? if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().configDir
        *///? } else if neoforge {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
        //? } else if forge {
        /*return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get()
        *///? }
    }

    fun gameDir(): Path {
        //? fabric
        //return net.fabricmc.loader.api.FabricLoader.getInstance().gameDir
        //? neoforge
        return net.neoforged.fml.loading.FMLPaths.GAMEDIR.get()
        //? forge
        //return net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get()
    }
}
