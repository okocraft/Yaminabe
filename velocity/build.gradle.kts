plugins {
    alias(libs.plugins.bundler)
}

jcommon {
    setupPaperRepository()
}

dependencies {
    implementation(projects.yaminabeCommon)
    compileOnly(libs.velocity)
}

bundler {
    copyToRootBuildDirectory("Yaminabe-Velocity-${project.version}")
    replacePluginVersionForVelocity(project.version)
}
