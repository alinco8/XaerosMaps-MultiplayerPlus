package dev.alinco8.xmmp.core.config

import com.akuleshov7.ktoml.Toml
import dev.alinco8.xmmp.core.LOGGER
import dev.alinco8.xmmp.core.network.XMMPStreamCodec
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class ServerConfig(
    val enableChunkValidation: Boolean = true,
    val regionFlushInterval: Long = 30_000L,
    val maxDownloadWindow: Int = 256,
    val downloadRateLimit: Double = 200.0,
    val downloadBurst: Double = 64.0,
    val shared: SharedConfig = SharedConfig(),
) {
    companion object {
        fun fromToml(file: File): ServerConfig {
            if (!file.exists()) return ServerConfig().also {
                it.saveToToml(file)
            }

            try {
                return Toml.decodeFromString<ServerConfig>(file.readText())
            } catch (e: Exception) {
                LOGGER.error("Failed to load server config from ${file.absolutePath}", e)

                val backup = File(
                    file.parentFile,
                    "${file.nameWithoutExtension}.bak.${System.currentTimeMillis()}.toml"
                )
                
                runCatching {
                    file.copyTo(backup)
                }.onFailure {
                    LOGGER.error(
                        "Failed to backup the invalid config to ${backup.absolutePath}",
                        it
                    )
                }

                LOGGER.info("Backed up the invalid config to ${backup.absolutePath}")

                return ServerConfig().also {
                    it.saveToToml(file)
                }
            }
        }
    }

    private fun saveToToml(file: File) {
        file.parentFile.mkdirs()
        file.writeText(Toml.encodeToString(this))
    }

    @Serializable
    data class SharedConfig(
        val syncCaves: Boolean = true,
        val uploadWindow: Int = 64,
        val uploadRateLimit: Double = 200.0,
        val uploadBurst: Double = 64.0,
        val regionRequestRateLimit: Double = 10.0,
        val regionRequestBurst: Double = 20.0,
    ) {
        companion object {
            val codec = with(XMMPStreamCodec) {
                composite(
                    boolean, SharedConfig::syncCaves,
                    int, SharedConfig::uploadWindow,
                    double, SharedConfig::uploadRateLimit,
                    double, SharedConfig::uploadBurst,
                    double, SharedConfig::regionRequestRateLimit,
                    double, SharedConfig::regionRequestBurst,
                    ::SharedConfig
                )
            }
        }
    }
}
