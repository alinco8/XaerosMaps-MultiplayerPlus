package dev.alinco8.xmmp.config

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Controller
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.ControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import dev.isxander.yacl3.api.controller.LongFieldControllerBuilder
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
    default: KProperty0<T>,
    current: KMutableProperty0<T>,
    controller: (Option<T>) -> ControllerBuilder<T>
) = option(
    Option.createBuilder<T>()
        .name(t("categories.general.options.${current.name}.name"))
        .description(OptionDescription.of(t("categories.general.options.${current.name}.description")))
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

        return YetAnotherConfigLib.createBuilder()
            .title(t("title"))
            .save {
                h.save()
            }
            .category(
                ConfigCategory.createBuilder()
                    .name(t("categories.general.name"))
                    .simpleOption(
                        d::chunkSendLimit,
                        i::chunkSendLimit,
                        IntegerFieldControllerBuilder::create
                    )
                    .simpleOption(
                        d::chunkApplyLimit,
                        i::chunkApplyLimit,
                        IntegerFieldControllerBuilder::create
                    )
                    .simpleOption(
                        d::maxRetries,
                        i::maxRetries,
                        IntegerFieldControllerBuilder::create
                    )
                    .simpleOption(
                        d::flushInterval,
                        i::flushInterval,
                        LongFieldControllerBuilder::create
                    )
                    .simpleOption(
                        d::maxChunkUploadsPerSecond,
                        i::maxChunkUploadsPerSecond,
                        IntegerFieldControllerBuilder::create
                    )
                    .simpleOption(
                        d::maxChunkRowRequestsPerSecond,
                        i::maxChunkRowRequestsPerSecond,
                        IntegerFieldControllerBuilder::create
                    )
                    .build()
            )
            .build()
            .generateScreen(parent)
    }
}
