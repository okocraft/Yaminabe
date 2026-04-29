plugins {
    alias(libs.plugins.bundler)
    alias(libs.plugins.run.server)
}

val minecraftVersion = "1.21"

dependencies {
    implementation(projects.yaminabeCommon)
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
            url("https://download.luckperms.net/1549/bukkit/loader/LuckPerms-Bukkit-5.4.134.jar")
        }

        args("--port=25560")
    }
}
