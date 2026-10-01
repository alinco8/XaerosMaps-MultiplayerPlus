package dev.alinco8.xmmp.config

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.network.XMMPStreamCodec
import dev.alinco8.xmmp.utils.ModPaths
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry

class ServerConfig {
    companion object {
        fun createHandler(): ConfigClassHandler<ServerConfig> =
            ConfigClassHandler.createBuilder(ServerConfig::class.java)
                .id(XMMP.loc("server_config"))
                .serializer { config ->
                    TomlConfigSerializer.Builder.create(config)
                        .setPath(ModPaths.configDir().resolve("${XMMP.MOD_ID}-server.toml"))
                        .build()
                }
                .build()

        val codecForClient = XMMPStreamCodec.of(
            { buf ->
                ServerConfig().apply {
                    syncCaves = buf.readBoolean()
                    uploadWindow = buf.readInt()
                    uploadRateLimit = buf.readDouble()
                    uploadBurst = buf.readDouble()
                    regionRequestRateLimit = buf.readDouble()
                    regionRequestBurst = buf.readDouble()
                }
            },
            { buf, config ->
                buf.writeBoolean(config.syncCaves)
                buf.writeInt(config.uploadWindow)
                buf.writeDouble(config.uploadRateLimit)
                buf.writeDouble(config.uploadBurst)
                buf.writeDouble(config.regionRequestRateLimit)
                buf.writeDouble(config.regionRequestBurst)
            }
        )
    }

    @SerialEntry(comment = "Validate uploaded map data on the server (recommended; small CPU cost)")
    var enableChunkValidation = true

    @SerialEntry(
        comment = "Sync underground maps (required if you want to sync the Nether or underground, " +
                "but enabling this will disable the cave layer feature)"
    )
    var syncCaves = true

    @SerialEntry(comment = "Interval (ms) between flushes of map data to disk")
    var regionFlushInterval = 30_000L

    @SerialEntry(comment = "Max chunk uploads a player may have in flight (server-side backlog per player)")
    var uploadWindow = 64

    @SerialEntry(comment = "Upper bound of chunk uploads per second per player")
    var uploadRateLimit = 200.0

    @SerialEntry(comment = "Burst size for the upload rate limit")
    var uploadBurst = 64.0

    @SerialEntry(comment = "Max chunk downloads a player may have in flight (caps the window requested by the client)")
    var maxDownloadWindow = 256

    @SerialEntry(
        comment = "Upper bound of chunk downloads per second per player. " +
                "Lower this if the server's upload bandwith is limited (~4 kb per chunk)."
    )
    var downloadRateLimit = 200.0

    @SerialEntry(comment = "Burst size for the download rate limit")
    var downloadBurst = 64.0

    @SerialEntry(comment = "Upper bound of region sync requests per second per player")
    var regionRequestRateLimit = 10.0

    @SerialEntry(comment = "Burst size for region sync requests")
    var regionRequestBurst = 20.0
}
