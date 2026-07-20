package buildlogic

import org.gradle.api.Project
import org.gradle.internal.extensions.core.extra
import org.gradle.kotlin.dsl.dependencies

fun Project.propList(key: String): List<String> {
    val result = mutableListOf<String>()
    var index = 0
    while (true) {
        val value = propOrNull("$key.$index") ?: break
        result.add(value)
        index++
    }

    return result
}

fun Project.propOrNull(key: String): String? =
    findProperty(key) as? String ?: System.getenv(key)

fun Project.prop(key: String): String =
    propOrNull(key) ?: run {
        error("Property $key not found")
    }

fun <T> Project.ifProp(key: String, block: (String) -> T): T? = propOrNull(key)?.run(block)

fun Project.modImplementation(
    dependencyNotation: Any,
) {
    val configuration = when (val loader = project.extra["loader"]) {
        "fabric", "forge" -> "modImplementation"
        "neoforge" -> "implementation"
        else -> error("Unknown loader: $loader")
    }

    dependencies {
        add(configuration, dependencyNotation)
    }
}
