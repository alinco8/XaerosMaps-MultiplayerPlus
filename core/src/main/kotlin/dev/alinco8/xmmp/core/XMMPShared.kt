package dev.alinco8.xmmp.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val LOGGER: Logger = LoggerFactory.getLogger(XMMPShared.MOD_ID)

object XMMPShared {
    const val MOD_ID = "xmmp"
    const val PACKET_VERSION = "3"

    fun id(path: String) = ResourceId(MOD_ID, path)
}
