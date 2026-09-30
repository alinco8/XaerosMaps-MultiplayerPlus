package dev.alinco8.xmmp.config

import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.UnmodifiableConfig
import com.electronwill.nightconfig.toml.TomlFormat
import com.electronwill.nightconfig.toml.TomlParser
import com.electronwill.nightconfig.toml.TomlWriter
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import dev.alinco8.xmmp.XMMP.LOGGER
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.ConfigField
import dev.isxander.yacl3.config.v2.api.ConfigSerializer
import dev.isxander.yacl3.config.v2.api.FieldAccess
import dev.isxander.yacl3.config.v2.api.SerialEntry
import java.io.StringWriter
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

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
) : ConfigSerializer<T>(config) {
    private class Entry(
        val name: String,
        val comment: String?,
        val type: Type,
        val get: () -> Any?,
        val set: (Any?) -> Unit,
    )

    class Builder<T>(private val config: ConfigClassHandler<T>) {
        private var path: Path? = null

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
            )
        }
    }

    override fun save() {
        try {
            val toml = CommentedConfig.of(TomlFormat.instance())
            write(toml, emptyList(), rootEntries { it.access() })

            val writer = StringWriter()
            TomlWriter().write(toml, writer)

            Files.createDirectories(path.parent)

            val tmpFile = Files.createTempFile(path.parent, "config", ".toml")
            try {
                Files.writeString(
                    tmpFile,
                    writer.toString(),
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE,
                )
                Files.move(tmpFile, path, StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(tmpFile)
            }
        } catch (e: Exception) {
            LOGGER.error(
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
            save()
            return LoadResult.NO_CHANGE
        }

        return try {
            val toml = Files.newBufferedReader(path).use { reader ->
                TomlParser().parse(reader)
            }

            if (read(
                    toml,
                    "",
                    rootEntries { bufferAccessMap[it] })
            ) LoadResult.DIRTY else LoadResult.SUCCESS
        } catch (e: Exception) {
            LOGGER.error(
                "Failed to parse config file '{}'. Using default values.",
                path,
                e
            )

            LoadResult.FAILURE
        }
    }

    private fun rootEntries(access: (ConfigField<*>) -> FieldAccess<*>?): List<Entry> =
        config.fields().mapNotNull { f ->
            val serial = f.serial().orElse(null) ?: return@mapNotNull null
            @Suppress("UNCHECKED_CAST")
            val a = access(f) as? FieldAccess<Any?>? ?: return@mapNotNull null
            Entry(
                serial.serialName(),
                serial.comment().orElse(null),
                a.type(),
                { a.get() },
                { a.set(it) })
        }

    private fun nestedEntries(obj: Any): List<Entry> =
        obj.javaClass.declaredFields.mapNotNull { f ->
            val e = f.getAnnotation(SerialEntry::class.java) ?: return@mapNotNull null
            if (Modifier.isStatic(f.modifiers)) return@mapNotNull null
            f.isAccessible = true
            Entry(
                e.value.ifEmpty { f.name },
                e.comment.ifEmpty { null },
                f.genericType,
                { f.get(obj) },
                { f.set(obj, it) })
        }

    private fun isSection(type: Type): Boolean = type is Class<*> && type.declaredFields.any {
        it.isAnnotationPresent(SerialEntry::class.java)
    }

    private fun children(e: Entry) =
        if (isSection(e.type)) e.get()?.let { nestedEntries(it) } else null

    private fun read(table: UnmodifiableConfig, prefix: String, entries: List<Entry>): Boolean {
        var dirty = false

        for (e in entries) {
            if (!table.contains(e.name)) {
                dirty = true
                continue
            }

            val raw = table.get<Any?>(e.name)
            val sub = children(e)
            if (sub != null) {
                if (raw is UnmodifiableConfig) {
                    if (read(raw, "$prefix${e.name}.", sub)) dirty = true
                } else {
                    dirty = true
                }
                continue
            }

            try {
                e.set(gson.fromJson(tomlToJson(raw), e.type))
            } catch (err: Exception) {
                LOGGER.error(
                    "Failed to deserialize config field '{}{}'. Using default value.",
                    prefix,
                    e.name,
                    err
                )
                dirty = true
            }
        }

        return dirty
    }

    private fun write(toml: CommentedConfig, prefix: List<String>, entries: List<Entry>) {
        for (e in entries) {
            val p = prefix + e.name
            val sub = children(e)

            if (sub != null) {
                write(toml, p, sub)
            } else {
                val value = try {
                    jsonToToml(gson.toJsonTree(e.get(), e.type))
                } catch (err: Exception) {
                    LOGGER.error(
                        "Failed to serialize config field '{}'. Skipping.",
                        p.joinToString("."),
                        err
                    )
                    null
                } ?: continue

                toml.set(p, value)
            }

            e.comment?.let { comment ->
                toml.setComment(
                    p,
                    comment.lines().joinToString("\n") { " $it" }
                )
            }
        }
    }

    private val gson = Gson()

    private fun jsonToToml(e: JsonElement): Any? = when {
        e.isJsonNull -> null
        e.isJsonPrimitive -> e.asJsonPrimitive.let { p ->
            when {
                p.isBoolean -> p.asBoolean
                p.isNumber -> when (val n = p.asNumber) {
                    is Byte, is Short, is Int, is Long -> n.toLong()
                    is Float -> n.toString().toDouble()
                    else -> n.toDouble()
                }

                else -> p.asString
            }
        }

        e.isJsonArray -> e.asJsonArray.mapNotNull { jsonToToml(it) }
        else -> CommentedConfig.of(TomlFormat.instance()).also { table ->
            for ((k, v) in e.asJsonObject.entrySet()) {
                jsonToToml(v)?.let { table.set<Any?>(listOf(k), it) }
            }
        }
    }

    private fun tomlToJson(v: Any?): JsonElement = when (v) {
        null -> JsonNull.INSTANCE
        is Boolean -> JsonPrimitive(v)
        is Number -> JsonPrimitive(v)
        is String -> JsonPrimitive(v)
        is List<*> -> JsonArray().also { arr -> v.forEach { arr.add(tomlToJson(it)) } }

        is UnmodifiableConfig -> JsonObject().also {
            for (entry in v.entrySet()) it.add(entry.key, tomlToJson(entry.getValue()))
        }

        else -> JsonPrimitive(v.toString())
    }
}
