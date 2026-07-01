package dev.alinco8.xmmp.config

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.loc
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry

@Suppress("MagicNumber")
class XMMPConfig {
    companion object {
        val HANDLER: ConfigClassHandler<XMMPConfig> =
            ConfigClassHandler.createBuilder(XMMPConfig::class.java)
                .id(loc("config"))
                .serializer { config ->
                    //? if fabric {
                    val configDir =
                        net.fabricmc.loader.api.FabricLoader.getInstance().configDir
                    //? } else if neoforge {
                    /*val configDir =
                        net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get()
                    *///? }

                    TomlConfigSerializer.Builder.create(config)
                        .setPath(configDir.resolve("${XMMP.MOD_ID}.toml"))
                        .build()
                }
                .build()
    }

    @SerialEntry(comment = "Maximum number of chunks to send (per tick)")
    var chunkSendLimit = 8

    @SerialEntry(comment = "Maximum number of chunks to apply (per tick)")
    var chunkApplyLimit = 32

    @SerialEntry(comment = "Maximum number of retries for sending/receiving a chunk before giving up")
    var maxRetries = 20

    @SerialEntry(comment = "Maximum number of pending chunk requests during synchronization")
    var maxPendingSyncChunks = 64

    @SerialEntry(comment = "Interval (in milliseconds) to clean up the unnecessary data")
    var flushInterval = 5000L

    @SerialEntry(comment = "Maximum number of chunk uploads the server accepts from a single player per second")
    var maxChunkUploadsPerSecond = 512

    @SerialEntry(comment = "Maximum number of chunk row requests the server accepts from a single player per second")
    var maxChunkRowRequestsPerSecond = 64
}
