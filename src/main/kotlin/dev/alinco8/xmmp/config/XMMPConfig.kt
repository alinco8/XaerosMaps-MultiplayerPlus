package dev.alinco8.xmmp.config

import dev.alinco8.xmmp.XMMP
import dev.alinco8.xmmp.utils.ModPaths
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry

@Suppress("MagicNumber")
class XMMPConfig {
    companion object {
        val HANDLER: ConfigClassHandler<XMMPConfig> =
            ConfigClassHandler.createBuilder(XMMPConfig::class.java)
                .id(XMMP.loc("config"))
                .serializer { config ->
                    TomlConfigSerializer.Builder.create(config)
                        .setPath(ModPaths.configDir().resolve("${XMMP.MOD_ID}.toml"))
                        .build()
                }
                .build()
    }

    @SerialEntry(comment = "Interval in milliseconds to flush the region stores")
    var flushInterval = 5000L

    @SerialEntry(comment = "Check for updates")
    var checkUpdate = true

    @SerialEntry(comment = "Max chunk downloads to have in flight at once (the server may cap this)")
    var downloadWindow = 128
}
