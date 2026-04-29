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
        compileOnlyApi(libs.slf4j.api)

        testImplementation(libs.junit.jupiter)
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
