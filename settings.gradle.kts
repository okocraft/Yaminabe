pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.0")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "yaminabe"

addProject("core")
addProject("plugin")

fun addProject(name: String) {
    addProject(name, name)
}

fun addProject(name: String, dir: String) {
    include("${rootProject.name}-$name")
    project(":${rootProject.name}-$name").projectDir = file(dir)
}
