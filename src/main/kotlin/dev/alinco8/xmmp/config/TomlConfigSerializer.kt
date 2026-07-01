package dev.alinco8.xmmp.config

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.UnmodifiableConfig
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.toml.TomlParser
import com.electronwill.nightconfig.toml.TomlWriter
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.mojang.serialization.JsonOps
import dev.alinco8.xmmp.XMMP
import dev.isxander.yacl3.config.util.CodecSerializerAdapter
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.ConfigField
import dev.isxander.yacl3.config.v2.api.ConfigSerializer
import dev.isxander.yacl3.config.v2.api.FieldAccess
import dev.isxander.yacl3.gui.utils.ItemRegistryHelper
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.chat.Style
import net.minecraft.world.item.Item
import java.awt.Color
import java.io.StringWriter
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.function.UnaryOperator

@SuppressWarnings(
    "TooGenericExceptionCaught",
    "LoopWithTooManyJumpStatements",
    "CyclomaticComplexMethod",
    "LongMethod",
    "ReturnCount",
    "MaxLineLength"
)
class TomlConfigSerializer<T> private constructor(
    config: ConfigClassHandler<T>,
    private val path: Path,
    private val gson: Gson,
) : ConfigSerializer<T>(config) {

    override fun save() {
        XMMP.LOGGER.info("Serializing {} to '{}'", config.configClass(), path)

        try {
            val toml = CommentedConfig.of(TomlFormat.instance())

            for (field in config.fields()) {
                val serial = field.serial().orElse(null) ?: continue
                val name = serial.serialName()

                val element = try {
                    gson.toJsonTree(field.access().get(), field.access().type())
                } catch (e: Exception) {
                    XMMP.LOGGER.error(
                        "Failed to serialize config field '{}'. Skipping because TOML has no null value.",
                        name,
                        e,
                    )
                    continue
                }

                val tomlValue = jsonToTomlValue(element) ?: continue

                toml.set<Any?>(listOf(name), tomlValue)

                serial.comment().orElse(null)?.let { comment ->
                    toml.setComment(listOf(name), formatTomlComment(comment))
                }
            }

            val stringWriter = StringWriter()
            TomlWriter().write(toml, stringWriter)

            Files.createDirectories(path.parent)
            Files.writeString(
                path,
                stringWriter.toString(),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
            )
        } catch (e: Exception) {
            XMMP.LOGGER.error(
                "Failed to serialize config class '{}'.",
                config.configClass().simpleName,
                e
            )
        }
    }

    override fun loadSafely(
        bufferAccessMap: Map<ConfigField<*>, FieldAccess<*>>,
    ): LoadResult {
        if (!Files.exists(path)) {
            XMMP.LOGGER.info(
                "Config file '{}' does not exist. Creating it with default values.",
                path
            )
            save()
            return LoadResult.NO_CHANGE
        }

        XMMP.LOGGER.info("Deserializing {} from '{}'", config.configClass().simpleName, path)

        val fieldMap = config.fields()
            .filter { it.serial().isPresent }
            .associateBy { it.serial().orElseThrow().serialName() }

        val missingFields = fieldMap.keys.toMutableSet()
        var dirty = false

        try {
            val toml = Files.newBufferedReader(path).use { reader ->
                TomlParser().parse(reader)
            }

            for (entry in toml.entrySet()) {
                val name = entry.key
                val rawTomlValue = entry.getValue<Any?>()

                val field = fieldMap[name]
                missingFields.remove(name)

                if (field == null) {
                    XMMP.LOGGER.warn("Found unknown config field '{}'.", name)
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                val bufferAccess = bufferAccessMap[field] as FieldAccess<Any?>? ?: continue

                val serial = field.serial().orElse(null) ?: continue

                val element = try {
                    tomlValueToJson(rawTomlValue)
                } catch (e: Exception) {
                    XMMP.LOGGER.error(
                        "Failed to deserialize config field '{}'. Due to the error state loading will be aborted.",
                        name,
                        e,
                    )
                    return LoadResult.FAILURE
                }

                if (element.isJsonNull && !serial.nullable()) {
                    XMMP.LOGGER.warn(
                        "Found null value in non-nullable config field '{}'. Leaving field as default and marking as dirty.",
                        name,
                    )
                    dirty = true
                    continue
                }

                try {
                    bufferAccess.set(gson.fromJson(element, bufferAccess.type()))
                } catch (e: Exception) {
                    XMMP.LOGGER.error(
                        "Failed to deserialize config field '{}'. Leaving as default.",
                        name,
                        e
                    )
                }
            }
        } catch (e: Exception) {
            XMMP.LOGGER.error("Failed to deserialize config class.", e)
            return LoadResult.FAILURE
        }

        if (missingFields.isNotEmpty()) {
            for (missingField in missingFields) {
                if (fieldMap[missingField]?.serial()?.orElseThrow()?.required() == true) {
                    dirty = true
                    XMMP.LOGGER.warn(
                        "Missing required config field '{}'. Re-saving as default.",
                        missingField
                    )
                }
            }
        }

        return if (dirty) LoadResult.DIRTY else LoadResult.SUCCESS
    }

    class Builder<T>(private val config: ConfigClassHandler<T>) {
        private var path: Path? = null

        private var gsonBuilder: UnaryOperator<GsonBuilder> = UnaryOperator { builder ->
            builder
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .serializeNulls()
                .registerTypeHierarchyAdapter(
                    Component::class.java,
                    CodecSerializerAdapter(ComponentSerialization.CODEC),
                )
                .registerTypeHierarchyAdapter(Style::class.java, StyleTypeAdapter())
                .registerTypeHierarchyAdapter(Color::class.java, ColorTypeAdapter())
                .registerTypeHierarchyAdapter(Item::class.java, ItemTypeAdapter())
                .setPrettyPrinting()
        }

        companion object {
            fun <T> create(config: ConfigClassHandler<T>) = Builder(config)
        }

        fun setPath(path: Path): Builder<T> {
            this.path = path
            return this
        }

        fun build(): ConfigSerializer<T> {
            return TomlConfigSerializer(
                config,
                path ?: error("`path` must be set before building the TomlConfigSerializer."),
                gsonBuilder.apply(GsonBuilder()).create(),
            )
        }
    }

    class StyleTypeAdapter : JsonSerializer<Style>, JsonDeserializer<Style> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): Style {
            return Style.Serializer.CODEC
                .parse(JsonOps.INSTANCE, json)
                .result()
                .orElse(Style.EMPTY)
        }

        override fun serialize(
            src: Style,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            return Style.Serializer.CODEC
                .encodeStart(JsonOps.INSTANCE, src)
                .result()
                .orElse(JsonNull.INSTANCE)
        }
    }

    class ColorTypeAdapter : JsonSerializer<Color>, JsonDeserializer<Color> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): Color {
            return Color(json.asInt, true)
        }

        override fun serialize(
            src: Color,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            return JsonPrimitive(src.rgb)
        }
    }

    class ItemTypeAdapter : JsonSerializer<Item>, JsonDeserializer<Item> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext,
        ): Item {
            return ItemRegistryHelper.getItemFromName(json.asString)
        }

        override fun serialize(
            src: Item,
            typeOfSrc: Type,
            context: JsonSerializationContext,
        ): JsonElement {
            return JsonPrimitive(BuiltInRegistries.ITEM.getKey(src).toString())
        }
    }
}

private fun jsonToTomlValue(element: JsonElement): Any? {
    return when {
        element.isJsonNull -> null

        element.isJsonPrimitive -> {
            val primitive = element.asJsonPrimitive

            when {
                primitive.isString -> primitive.asString
                primitive.isBoolean -> primitive.asBoolean
                primitive.isNumber -> parseTomlNumber(primitive.asString)
                else -> primitive.asString
            }
        }

        element.isJsonArray -> {
            element.asJsonArray.map { jsonToTomlValue(it) }
        }

        element.isJsonObject -> {
            val config = CommentedConfig.of(TomlFormat.instance())

            for ((key, value) in element.asJsonObject.entrySet()) {
                val tomlValue = jsonToTomlValue(value)

                if (tomlValue != null) {
                    config.set<Any?>(listOf(key), tomlValue)
                }
            }

            config
        }

        else -> element.toString()
    }
}

private fun tomlValueToJson(value: Any?): JsonElement {
    return when (value) {
        null -> JsonNull.INSTANCE

        is String -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)

        is List<*> -> {
            JsonArray().also { array ->
                value.forEach { array.add(tomlValueToJson(it)) }
            }
        }

        is UnmodifiableConfig -> {
            JsonObject().also { obj ->
                for (entry in value.entrySet()) {

                    obj.add(entry.key, tomlValueToJson(entry.getValue()))
                }
            }
        }

        else -> JsonPrimitive(value.toString())
    }
}

private fun parseTomlNumber(value: String): Number {
    return if (
        value.contains('.') ||
        value.contains('e', ignoreCase = true)
    ) {
        value.toDouble()
    } else {
        value.toLong()
    }
}

private fun formatTomlComment(comment: String): String {
    return comment
        .lineSequence()
        .joinToString("\n") { line -> " $line" }
}
