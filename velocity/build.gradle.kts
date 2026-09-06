plugins {
    alias(libs.plugins.bundler)
    alias(libs.plugins.run.velocity)
}

apply(from = rootProject.file("gradle/merge-languages.gradle.kts"))

jcommon {
    setupPaperRepository()
}

dependencies {
    implementation(projects.yaminabeCommon)
    compileOnly(libs.velocity)
    testImplementation(libs.velocity)
}

bundler {
    copyToRootBuildDirectory("Yaminabe-Velocity-${project.version}")
    replacePluginVersionForVelocity(project.version)
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.get())
    }
}
