import java.text.SimpleDateFormat
import java.util.Date

plugins {
    /** forge & mappings */
    alias(libs.plugins.forgegradle)
    alias(libs.plugins.parchment.forgegradle)

    /** mixin */
    alias(libs.plugins.mixin)

    /** ide */
    eclipse
    idea

    /** publish */
    `maven-publish`
    alias(libs.plugins.mod.publish.plugin)
}

val mcVersion = libs.versions.minecraft.get()

version = "${prop("mod_version")}+$mcVersion-${prop("mod_loader").lowercase()}"
group = prop("mod_group")

base {
    archivesName = prop("mod_filename")
}

repositories {

}

dependencies {
    minecraft(forge())
    annotationProcessor(libs.mixin.classifier("processor"))


}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(prop("java_version"))
    }

    withSourcesJar()
}

minecraft {
    mappings(
        prop("mapping_channel"),
        prop("mapping_version")
    )

//    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    copyIdeResources = true
    generateRunFolders = true

    runs {
        configureEach {
            workingDirectory = project.file("run").path

            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            mods {
                create(prop("mod_id")) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            property("forge.enabledGameTestNamespaces", prop("mod_id"))

            if (canSpecifyUser()) {
                args(
                    "--username", prop("mc_username"),
                    "--uuid", prop("mc_uuid")
                )
            }
        }

        create("server") {
            property("forge.enabledGameTestNamespaces", prop("mod_id"))
            args("--nogui")
        }

        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", prop("mod_id"))
        }

        create("data") {
            workingDirectory = project.file("run-data").path

            args(
                "--mod", prop("mod_id"),
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources/")
            )
        }
    }
}

/*
mixin {
    add(
        sourceSets.main.get(), "${prop("mod_id")}.refmap.json"
    )

    config("${prop("mod_id")}.mixins.json")
}
*/

sourceSets {
    main {
        resources {
            srcDir("src/generated/resources")
        }
    }
}

tasks {
    processResources {
        val replaceProperties = mapOf(
            "mod_group"               to project.group,
            "mod_version"             to project.version,
            "minecraft_version_range" to prop("minecraft_version_range"),
            "forge_version_range"     to prop("forge_version_range"),
            "loader_version_range"    to prop("loader_version_range"),
            "mod_id"                  to prop("mod_id"),
            "mod_name"                to prop("mod_name"),
        )

        inputs.properties(replaceProperties)

        filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
            expand(replaceProperties + mapOf("project" to project))
        }
    }

    jar {
        manifest {
            attributes(
                "Specification-Title"      to prop("mod_name"),
                "Specification-Vendor"     to prop("mod_authors"),
                "Specification-Version"    to "1",
                "Implementation-Title"     to project.name,
                "Implementation-Version"   to project.version,
                "Implementation-Vendor"    to prop("mod_authors"),
                "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())
            )
        }

        finalizedBy("reobfJar")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

eclipse {
    synchronizationTasks("genEclipseRuns")
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val releaseTitle = "${prop("mod_name")} ${prop("mod_version")} for ${prop("mod_loader")} $mcVersion"
// val changelogFile = file("changelog.md")

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifact(tasks.jar)
        }
    }

    repositories {
        maven {
            url = uri("file://${project.projectDir}/mcmodsrepo")
        }
    }
}

/*
publishMods {
    file = modJar()
    version = "${project.version}"

    changelog = changelogFile.readText()

    modLoaders.add("forge")
    modLoaders.add("neoforge")

    type = STABLE

    github {
        accessToken = System.getenv("GITHUB_TOKEN")

        repository = "test/example"
        commitish = "main"

        tagName = "v" + project.version
        displayName = tagName

        additionalFiles.from(sourcesJar())
    }

    curseforge {
        accessToken = System.getenv("CURSEFORGE_TOKEN")

        projectId = "123456"
        projectSlug = "example"

        displayName = releaseTitle

        javaVersions.add(JavaVersion.VERSION_17)
        minecraftVersions.add("1.20.1")

        client = true
        server = true

        additionalFiles.from(sourcesJar())
    }

    modrinth {
        accessToken = System.getenv("MODRINTH_TOKEN")

        projectId = "example"

        displayName = releaseTitle

        minecraftVersions.add("1.20.1")

        environment = CLIENT_AND_SERVER

        additionalFile(sourcesJar()) {
            type = SOURCES_JAR
        }
    }
}
*/

fun prop(key: String): String {
    return properties[key].toString()
}

fun extra(key: String): String {
    return extra[key].toString()
}

fun forge(): String {
    return buildString {
        append("net.minecraftforge")
        append(":")
        append("forge")
        append(":")
        append(mcVersion)
        append("-")
        append(libs.versions.forge.get())
    }
}

fun Provider<MinimalExternalModuleDependency>.classifier(name: String): String {
    return "${this.get()}:$name"
}

fun canSpecifyUser(): Boolean {
    return hasProperty("mc_username") && hasProperty("mc_uuid")
}

fun modJar(): RegularFile {
    return tasks.jar.get().archiveFile.get()
}

fun sourcesJar(): RegularFile {
    return tasks.named<Jar>("sourcesJar").get().archiveFile.get()
}
