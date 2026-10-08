package dev.alinco8.xmmp

//? if >=1.21.11 {
/*import net.minecraft.resources.Identifier

*///? } else {
import net.minecraft.resources.ResourceLocation as Identifier

//? }

import dev.alinco8.xmmp.config.XMMPConfig
import dev.alinco8.xmmp.core.ResourceId
import net.minecraft.resources.ResourceKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object XMMP {
    const val MOD_ID = "xmmp"
    const val PACKET_VERSION = "3"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    fun loc(path: String): Identifier {
        //? if forge || >=1.21 {
        return Identifier.fromNamespaceAndPath(MOD_ID, path)
        //? } else {
        /*return Identifier(MOD_ID, path)
        *///? }
    }

    fun onInitialize() {
        LOGGER.debug("Initializing XMMP")

        XMMPConfig.HANDLER.load()
    }
}

fun ResourceId.toIdentifier(): Identifier {
    //? if >=1.21 {
    return Identifier.fromNamespaceAndPath(this.namespace, this.path)
    //? } else {
    /*return Identifier(this.namespace, this.path)
    *///? }
}

fun Identifier.toResourceId(): ResourceId {
    return ResourceId(this.namespace, this.path)
}

fun ResourceKey<*>.id(): ResourceId {
    //? if >=1.21.11 {
    /*return this.identifier().toResourceId()
    *///? } else {
    return this.location().toResourceId()
    //? }
}
