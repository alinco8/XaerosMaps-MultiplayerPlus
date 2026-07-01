//? if fabric {
package dev.alinco8.xmmp.platform.fabric

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import dev.alinco8.xmmp.config.ConfigScreen

class ModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory() =
        ConfigScreenFactory { parent -> ConfigScreen.createScreen(parent) }
}
//? }
