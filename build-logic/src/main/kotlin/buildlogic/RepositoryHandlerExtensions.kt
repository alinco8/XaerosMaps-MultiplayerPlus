package buildlogic

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

fun RepositoryHandler.strictMaven(group: String, vararg urls: String) {
    exclusiveContent {
        urls.forEach { url ->
            forRepository {
                maven(url)
            }
        }
        @Suppress("UnstableApiUsage")
        filter {
            includeGroupAndSubgroups(group)
        }
    }
}
