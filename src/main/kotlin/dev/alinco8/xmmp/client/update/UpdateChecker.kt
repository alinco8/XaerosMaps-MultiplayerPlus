package dev.alinco8.xmmp.client.update

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.XMMP.LOGGER
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component

object UpdateChecker {
    //? if <=1.20.1 {
    /*private val updateToastId = SystemToast.SystemToastIds.PERIODIC_NOTIFICATION
    *///? } else {
    private val updateToastId = SystemToast.SystemToastId()
    //? }

    private val client = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private var lastChecked = 0L

    private fun getCurrentVersion(): String {
        //? if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(XMMP.MOD_ID)
            .orElseThrow().metadata.version.friendlyString
        *///? } else if forge {
        /*return net.minecraftforge.fml.ModList.get().getModContainerById(XMMP.MOD_ID)
            .orElseThrow().modInfo.version.toString()
        *///? } else if neoforge {
        return net.neoforged.fml.ModList.get().getModContainerById(XMMP.MOD_ID)
            .orElseThrow().modInfo.version.toString()
        //? }
    }

    private suspend fun getLatestVersion(): String? {
        val mcVersion = SharedConstants.getCurrentVersion().run {
            //? if <=1.21.5 {
            name
            //? } else {
            /*name()
            *///? }
        }

        val req = HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "https://api.modrinth.com/v3/project/stTaMuWa/version" +
                            "?include_changelog=false&game_versions=[%22" +
                            mcVersion +
                            "%22]&loaders=[%22" +
                            //? neoforge
                            "neoforge" +
                            //? forge
                            //"forge" +
                            //? fabric
                            //"fabric" +
                            "%22]"
                )
            )
            .GET()
            .build()

        val res = client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).await()
        val data = json.parseToJsonElement(res.body()).jsonArray

        val latest = data.firstOrNull() ?: return null

        return latest.jsonObject["version_number"]?.jsonPrimitive?.content
    }

    suspend fun checkForUpdates() {
        val now = System.currentTimeMillis()
        if (now - lastChecked < 60 * 60 * 1000) return
        lastChecked = now

        val currentVersion = getCurrentVersion()
        val latestVersion = getLatestVersion() ?: run {
            LOGGER.warn("Failed to fetch latest version from Modrinth API")
            return
        }

        //TODO: Compare versions properly
        if (currentVersion == latestVersion) return

        //? if >=26.2 {
        /*val toasts = Minecraft.getInstance().gui.toastManager()
        *///? } else if >=1.21.4 {
        /*val toasts = Minecraft.getInstance().toastManager
        *///? } else {
        val toasts = Minecraft.getInstance().toasts
        //? }

        LOGGER.info("Update available: $currentVersion -> $latestVersion")

        SystemToast.addOrUpdate(
            toasts,
            updateToastId,
            Component.literal("XMMP"),
            Component.literal("Update available: $latestVersion"),
        )
    }
}
