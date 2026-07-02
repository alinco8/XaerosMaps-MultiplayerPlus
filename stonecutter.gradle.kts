plugins {
    id("dev.kikugie.stonecutter")
    id("project.base")
}

stonecutter active rootProject.file("versions/active.txt")

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
