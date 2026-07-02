plugins {
    id("dev.kikugie.stonecutter")
    id("project.base")
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split("-", limit = 2)
    properties.tags(version, loader)

    constants.match(
        loader, "fabric", "neoforge", "forge"
    )
}

stonecutter handlers {
    inherit("cfg", "toml")
}

stonecutter tasks {
    order("publishModrinth")
    order("publishCurseforge")
}
