plugins {
    alias(libs.plugins.bundler)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.run.server)
}

val minecraftVersion = libs.versions.paper.get().replaceAfter(".build", "").removeSuffix(".build")

dependencies {
    implementation(projects.yaminabeCommon)

    compileOnlyApi(libs.paper)
    testImplementation(libs.paper)

    paperweight.paperDevBundle(libs.versions.paper.get())
}

bundler {
    copyToRootBuildDirectory("Yaminabe-Paper-${project.version}")
    replacePluginVersionForPaper(project.version)
}

tasks {
    test {
        // Bootstrapping a server replaces the standard streams with ones that log through slf4j, so the logger has to
        // hold on to the original streams beforehand, or writing a log line recurses until the stack overflows.
        systemProperty("org.slf4j.simpleLogger.cacheOutputStream", "true")
        // Lets TestServerExtension set the server up before any test touches an item.
        systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    }

    runServer {
        minecraftVersion(minecraftVersion)
        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("paper.disable-plugin-rewriting", "true")

        downloadPlugins {
            // See https://luckperms.net/download
            url("https://download.luckperms.net/1658/bukkit/loader/LuckPerms-Bukkit-5.5.71.jar")
        }
    }
}
