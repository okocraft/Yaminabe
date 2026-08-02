plugins {
    alias(libs.plugins.bundler)
    alias(libs.plugins.run.server)
}

val minecraftVersion = libs.versions.paper.get().replaceAfter(".build", "").removeSuffix(".build")

dependencies {
    implementation(projects.yaminabeCommon)

    compileOnlyApi(libs.paper)
}

bundler {
    copyToRootBuildDirectory("Yaminabe-Paper-${project.version}")
    replacePluginVersionForPaper(project.version)
}

tasks {
    runServer {
        minecraftVersion(minecraftVersion)
        systemProperty("com.mojang.eula.agree", "true")
        systemProperty("paper.disable-plugin-rewriting", "true")

        downloadPlugins {
            // See https://luckperms.net/download
            url("https://download.luckperms.net/1645/bukkit/loader/LuckPerms-Bukkit-5.5.57.jar")
        }
    }
}
