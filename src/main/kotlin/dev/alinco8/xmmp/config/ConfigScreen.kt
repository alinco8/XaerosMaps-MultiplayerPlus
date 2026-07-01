package dev.alinco8.xmmp.config

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private fun <T : Any> Option.Builder<T>.bind(
    default: KProperty0<T>,
    current: KMutableProperty0<T>,
) = binding(default.get(), { current.get() }, { current.set(it) })

object ConfigScreen {
    private fun t(text: String) = Component.translatable("xmmp.config.$text")

    @Suppress("MaxLineLength")
    fun createScreen(parent: Screen): Screen {
        val h = XMMPConfig.HANDLER
        val d = h.defaults()
        val i = h.instance()

        //TODO: add rest of the options
        return YetAnotherConfigLib.createBuilder()
            .title(t("title"))
            .save {
                h.save()
            }
            .category(
                ConfigCategory.createBuilder()
                    .name(t("categories.general.name"))
                    .option(
                        Option.createBuilder<Int>()
                            .name(t("categories.general.options.chunkSendLimit.name"))
                            .description(OptionDescription.of(t("categories.general.options.chunkSendLimit.description")))
                            .bind(d::chunkSendLimit, i::chunkSendLimit)
                            .controller(IntegerFieldControllerBuilder::create)
                            .build()
                    )
                    .option(
                        Option.createBuilder<Int>()
                            .name(t("categories.general.options.chunkApplyLimit.name"))
                            .description(OptionDescription.of(t("categories.general.options.chunkApplyLimit.description")))
                            .bind(d::chunkApplyLimit, i::chunkApplyLimit)
                            .controller(IntegerFieldControllerBuilder::create)
                            .build()
                    )
                    .option(
                        Option.createBuilder<Int>()
                            .name(t("categories.general.options.maxRetries.name"))
                            .description(OptionDescription.of(t("categories.general.options.maxRetries.description")))
                            .bind(d::maxRetries, i::maxRetries)
                            .controller(IntegerFieldControllerBuilder::create)
                            .build()
                    )
                    .build()
            )
            .build()
            .generateScreen(parent)
    }
}
