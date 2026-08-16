plugins {
    alias(libs.plugins.jcommon)
}

jcommon {
    javaVersion = JavaVersion.VERSION_25

    setupPaperRepository()
    setupJUnit(libs.junit.bom)
    setupMockito(libs.mockito)

    commonDependencies {
        compileOnlyApi(libs.annotations)
        compileOnlyApi(libs.configurate.yaml)
        compileOnlyApi(libs.slf4j.api)

        implementation(libs.mcmsgdef)

        testImplementation(libs.junit.jupiter)
        testImplementation(libs.adventure.api)
        testImplementation(libs.adventure.minimessage)
        testImplementation(libs.configurate.yaml)
        testRuntimeOnly(libs.slf4j.simple)
    }

    jarTask {
        manifest {
            attributes(
                "Implementation-Version" to project.version.toString()
            )
        }
    }
}
