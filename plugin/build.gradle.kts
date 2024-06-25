plugins {
    id("yaminabe.common-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.server)
}

val minecraftVersion = "1.21"

dependencies {
    implementation(projects.yaminabeCore)
}

tasks {
    build {
        dependsOn(shadowJar)
        doLast {
            val filepath = getArtifactFilepath()
            filepath.parentFile.mkdirs()
            shadowJar.get().archiveFile.get().asFile.copyTo(getArtifactFilepath(), true)
        }
    }

    clean {
        doLast {
            getArtifactFilepath().delete()
        }
    }

    processResources {
        filesMatching(listOf("paper-plugin.yml")) {
            expand("projectVersion" to project.version)
        }
    }

    shadowJar {
        mergeServiceFiles()
    }

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

fun getArtifactFilepath() : File {
    return rootProject.layout.buildDirectory.dir("libs").get().file("Yaminabe-${project.version}.jar").asFile
}
