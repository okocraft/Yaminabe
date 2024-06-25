import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

dependencies {
    compileOnlyApi(libs.annotations)
    compileOnly(libs.paper)

    testImplementation(libs.paper)

    testImplementation(libs.mockito)
    testRuntimeOnly(libs.slf4j.simple)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val javaVersion = JavaVersion.VERSION_21
val charset = Charsets.UTF_8

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion.ordinal + 1))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks {
    compileJava {
        options.encoding = charset.name()
        options.release.set(javaVersion.ordinal + 1)
    }

    processResources {
        filteringCharset = charset.name()
    }

    test {
        // See https://github.com/mockito/mockito/issues/3037
        jvmArgs("-XX:+EnableDynamicAgentLoading")

        useJUnitPlatform()

        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        }
    }
}
