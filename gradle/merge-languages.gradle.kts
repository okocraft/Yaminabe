import org.gradle.api.GradleException
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.util.Properties

fun loadLanguageKeys(file: File): Set<String> {
    val properties = Properties()
    file.reader(Charsets.UTF_8).use { properties.load(it) }
    return properties.stringPropertyNames()
}

val commonLanguageDirectory = rootProject.file("common/src/main/languages")
val platformLanguageDirectory = file("src/main/languages")
val generatedLanguageResources = layout.buildDirectory.dir("generated/language-resources")

val mergeLanguageResources = tasks.register("mergeLanguageResources") {
    inputs.dir(commonLanguageDirectory).optional()
    inputs.dir(platformLanguageDirectory).optional()
    outputs.dir(generatedLanguageResources)

    doLast {
        val outputRoot = generatedLanguageResources.get().asFile
        project.delete(outputRoot)

        val outputLanguageDirectory = outputRoot.resolve("languages")
        outputLanguageDirectory.mkdirs()

        val commonFiles = commonLanguageDirectory
            .listFiles { file -> file.isFile && file.extension == "properties" }
            ?.associateBy { it.name }
            .orEmpty()
        val platformFiles = platformLanguageDirectory
            .listFiles { file -> file.isFile && file.extension == "properties" }
            ?.associateBy { it.name }
            .orEmpty()

        val fileNames = (commonFiles.keys + platformFiles.keys).sorted()
        for (fileName in fileNames) {
            val commonFile = commonFiles[fileName]
            val platformFile = platformFiles[fileName]

            if (commonFile != null && platformFile != null) {
                val duplicateKeys = loadLanguageKeys(commonFile)
                    .intersect(loadLanguageKeys(platformFile))
                    .sorted()
                if (duplicateKeys.isNotEmpty()) {
                    throw GradleException(
                        buildString {
                            append("Duplicate language keys in ")
                            append(fileName)
                            append(" between common and ")
                            append(project.path)
                            append(":\n")
                            duplicateKeys.forEach { key ->
                                append(" - ")
                                append(key)
                                append('\n')
                            }
                        }.trimEnd()
                    )
                }
            }

            val contents = listOfNotNull(commonFile, platformFile)
                .map { it.readText(Charsets.UTF_8).trimEnd() }
                .filter { it.isNotEmpty() }
            if (contents.isNotEmpty()) {
                outputLanguageDirectory.resolve(fileName)
                    .writeText(contents.joinToString("\n\n", postfix = "\n"), Charsets.UTF_8)
            }
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(mergeLanguageResources)
    from(generatedLanguageResources)
}
