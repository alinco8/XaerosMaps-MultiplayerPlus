package buildlogic

import org.gradle.api.file.ContentFilterable

val templateRegex = Regex("""\$\{\s*(\w+(?:\.\w+)*)\s*(?:\|\s*(\w+))?\s*}""")
fun ContentFilterable.strictExpand(loader: String, props: Map<String, String>, filePath: String) {
    var lineNumber = 0

    filter { line ->
        lineNumber++

        if ($$"${" !in line) return@filter line

        val sb = StringBuilder(line.length)
        var lastEnd = 0

        for (match in templateRegex.findAll(line)) {
            sb.append(line, lastEnd, match.range.first)

            val key = match.groups[1]!!.value
            val fnName = match.groups[2]?.value ?: ""

            val value = when (fnName) {
                "versionRange" -> versionRangeProcessor(loader, props, key)
                "" -> props[key]
                    ?: error("Missing property '$key' in file $filePath at line $lineNumber")

                else -> error("Unknown function '$fnName' in file $filePath at line $lineNumber")
            }

            sb.append(
                value
            )
            lastEnd = match.range.last + 1
        }

        sb.append(line, lastEnd, line.length)
        sb.toString()
    }
}

fun versionRangeProcessor(loader: String, props: Map<String, String>, propName: String): String {
    props[propName]?.let { return it }

    val start = props["${propName}.0"]
        ?: error("Missing property '${propName}.0' for version range in loader '$loader'")
    val end = props["${propName}.1"]

    return when (loader) {
        "neoforge", "forge" -> "[$start,${end ?: ""})"
        "fabric" -> ">=$start${if (end != null) " <${end}" else ""}"

        else -> error("Unknown loader '$loader' for version range processing")
    }
}
