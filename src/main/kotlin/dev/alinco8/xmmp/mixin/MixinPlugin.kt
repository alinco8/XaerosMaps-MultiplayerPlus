package dev.alinco8.xmmp.mixin

import dev.alinco8.xmmp.utils.ModList
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

class MixinPlugin : IMixinConfigPlugin {
    lateinit var prefix: String

    override fun onLoad(mixinPackage: String) {
        prefix = mixinPackage
    }

    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String, mixinClassName: String): Boolean {
        val paths = mixinClassName.removePrefix("$prefix.").split('.')
        if (paths.firstOrNull() != "compat") return true
        val modId = paths.drop(1).firstOrNull() ?: return true

        return ModList.isModLoaded(modId)
    }

    override fun acceptTargets(myTargets: MutableSet<String>, otherTargets: MutableSet<String>) {}
    override fun getMixins(): MutableList<String>? = null
    override fun preApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }

    override fun postApply(
        targetClassName: String,
        targetClass: ClassNode,
        mixinClassName: String,
        mixinInfo: IMixinInfo
    ) {
    }
}
