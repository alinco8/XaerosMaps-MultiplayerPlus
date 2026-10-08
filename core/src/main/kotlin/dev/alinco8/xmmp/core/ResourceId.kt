package dev.alinco8.xmmp.core

data class ResourceId(val namespace: String, val path: String) {
    companion object {
        const val DEFAULT_NAMESPACE = "minecraft"
    }

    constructor(id: String) : this(DEFAULT_NAMESPACE, id)

    init {
        require(namespace.isNotEmpty()) { "Namespace cannot be empty" }
        require(namespace.all(::isValid)) { "Namespace contains invalid characters" }

        require(path.isNotEmpty()) { "Path cannot be empty" }
        require(path.all(::isValid)) { "Path contains invalid characters" }

        require(namespace.contains("..").not()) { "Path cannot contain '..'" }
        require(namespace.startsWith("/").not()) { "Path cannot start with '/'" }

        require(path.contains("..").not()) { "Path cannot contain '..'" }
        require(path.startsWith("/").not()) { "Path cannot start with '/'" }
    }

    private fun isValid(char: Char): Boolean =
        char.isLetterOrDigit() || char == '_' || char == '-' || char == '/' || char == '.'

    override fun toString() = "$namespace:$path"
}
