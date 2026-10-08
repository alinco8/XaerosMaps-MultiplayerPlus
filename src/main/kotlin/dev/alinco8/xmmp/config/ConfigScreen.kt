package dev.alinco8.xmmp.config

import dev.alinco8.xmmp.client.XMMPClient
import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Controller
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.ControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private fun <T : Any> Option.Builder<T>.bind(
    default: KProperty0<T>,
    current: KMutableProperty0<T>,
) = binding(default.get(), { current.get() }, { current.set(it) })

private fun t(text: String) = Component.translatable("xmmp.config.$text")

private fun <T : Any> ConfigCategory.Builder.simpleOption(
    category: String,
    default: KProperty0<T>,
    current: KMutableProperty0<T>,
    controller: (Option<T>) -> ControllerBuilder<T>,
) = option(
    Option.createBuilder<T>()
        .name(t("categories.$category.options.${current.name}.name"))
        .description(OptionDescription.of(t("categories.$category.options.${current.name}.description")))
        .bind(default, current)
        .controller(controller)
        .build()
)

object ConfigScreen {
    @Suppress("MaxLineLength")
    fun createScreen(parent: Screen): Screen {
        val h = XMMPConfig.HANDLER
        val d = h.defaults()
        val i = h.instance()

        var builder = YetAnotherConfigLib.createBuilder()
            .title(t("title"))
            .save {
                h.save()
            }
            .category(
                ConfigCategory.createBuilder()
                    .name(t("categories.general.name"))
                    .simpleOption(
                        "general",
                        d::flushInterval,
                        i::flushInterval,
                        LongFieldControllerBuilder::create
                    )
                    .simpleOption(
                        "general",
                        d::checkUpdate,
                        i::checkUpdate,
                        TickBoxControllerBuilder::create
                    )
                    .simpleOption(
                        "general",
                        d::downloadWindow,
                        i::downloadWindow,
                        IntegerFieldControllerBuilder::create
                    )
                    .build()
            )

        val worldId = XMMPClient.getWorldId()
        if (worldId != null) {
            val worldConfig = i.worlds.getOrPut(worldId) { XMMPConfig.WorldConfig() }
            val worldDefaults = XMMPConfig.WorldConfig()

            builder = builder.category(
                ConfigCategory.createBuilder()
                    .name(t("categories.world.name"))
                    .simpleOption(
                        "world",
                        worldDefaults::serverAddress,
                        worldConfig::serverAddress,
                        StringControllerBuilder::create
                    )
                    .build()
            )
        }

        return builder.build().generateScreen(parent)
    }
}
