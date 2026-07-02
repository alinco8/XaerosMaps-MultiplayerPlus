package buildlogic

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.ContentFilterable
import org.gradle.internal.extensions.core.extra

val templateRegex = Regex("""\$\{\s*(\w+(\.\w+)*)\s*}""")
fun ContentFilterable.strictExpand(props: Map<String, String>, filePath: String) {
    var lineNumber = 0

    filter { line ->
        lineNumber++

        if ($$"${" !in line) return@filter line

        val sb = StringBuilder(line.length)
        var lastEnd = 0

        for (match in templateRegex.findAll(line)) {
            sb.append(line, lastEnd, match.range.first)

            val key = match.groups[1]!!.value

            sb.append(
                props[key]
                    ?: error("Missing property '$key' in file $filePath at line $lineNumber")
            )
            lastEnd = match.range.last + 1
        }

        sb.append(line, lastEnd, line.length)
        sb.toString()
    }
}
