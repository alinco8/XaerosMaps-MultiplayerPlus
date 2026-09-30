package dev.alinco8.xmmp.common

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

interface ServerEvents {
    fun registerServerStarted(callback: (server: MinecraftServer) -> Unit)
    fun registerServerStopped(callback: () -> Unit)
    fun registerTickPost(callback: () -> Unit)

    fun registerPlayerChannelsReady(callback: (player: ServerPlayer) -> Unit)
    fun registerPlayerJoin(callback: (player: ServerPlayer) -> Unit)
    fun registerPlayerLeave(callback: (player: ServerPlayer) -> Unit)
    fun registerPlayerChangedDimension(callback: (player: ServerPlayer) -> Unit)

    fun registerCommands(callback: (dispatcher: CommandDispatcher<CommandSourceStack>) -> Unit)
}
