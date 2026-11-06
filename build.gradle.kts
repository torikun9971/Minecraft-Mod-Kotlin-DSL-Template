@file:Suppress("UnstableApiUsage")

import net.fabricmc.loom.api.mappings.layered.spec.LayeredMappingSpecBuilder

plugins {
    /** lang & loom */
    `java-library`
    alias(libs.plugins.fabric.loom)

    /** publish */
    `maven-publish`
    alias(libs.plugins.mod.publish.plugin)

    /** ide */
    eclipse
    idea
}

val mcVersion = libs.versions.minecraft.get()

group = prop("mod_group")
version = "${prop("mod_version")}+$mcVersion-${prop("mod_loader").lowercase()}"

base {
    archivesName.set(prop("mod_filename"))
}

repositories {
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
}

dependencies {
    minecraft(libs.minecraft)

    mappings(loom.layered {
        officialMojangMappings()
        parchmentMappings()
    })

    modImplementation(libs.fabric)
//    modImplementation(libs.fabric.api)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }

    withSourcesJar()
}

loom {
    /**
     * Don’t forget to add the following line to fabric.mod.json:
     *
     * "accessWidener": "${mod_id}.accesswidener",
     */
//    accessWidenerPath = file("src/main/resources/${prop("mod_id")}.accesswidener")

    runs {
        named("client") {
            if (canSpecifyUser()) {
                programArguments.addAll(
                    "--username", prop("mc_username"),
                    "--uuid", prop("mc_uuid")
                )
            }
        }
    }
}

tasks {
    processResources {
        val replaceProperties = mapOf(
            "mod_group"          to project.group,
            "mod_version"        to project.version,
            "java_version"       to prop("java_version"),
            "minecraft_version"  to mcVersion,
            "fabric_version"     to libs.versions.fabric.asProvider().get(),
            "fabric_api_version" to libs.versions.fabric.api.get(),
            "mod_id"             to prop("mod_id"),
            "mod_name"           to prop("mod_name"),
        )

        inputs.properties(replaceProperties)

        filesMatching(listOf("fabric.mod.json", "pack.mcmeta")) {
            expand(replaceProperties)
        }
    }

    jar {
//    from("LICENSE")
    }
}

tasks {
    val javaVersion = prop("java_version").toInt()

    withType<JavaCompile> {
        options.release.set(javaVersion)
    }
}

eclipse {
    synchronizationTasks("genSources")
}

tasks.matching { it.name == "ideaSyncTask" }.configureEach {
    dependsOn("genSources")
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
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {

    }
}

/*
publishMods {
    file = modJar()
    version = "${project.version}"

    changelog = changelogFile.readText()

    modLoaders.add("fabric")
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

        javaVersions.add(JavaVersion.VERSION_21)
        minecraftVersions.add("1.21.4")

        client = true
        server = true

        additionalFiles.from(sourcesJar())
    }

    modrinth {
        accessToken = System.getenv("MODRINTH_TOKEN")

        projectId = "example"

        displayName = releaseTitle

        minecraftVersions.add("1.21.4")

        environment = CLIENT_AND_SERVER

        additionalFile(sourcesJar()) {
            type = SOURCES_JAR
        }
    }
}
*/

fun prop(key: String): String {
    return providers.gradleProperty(key).get()
}

fun extra(key: String): String {
    return extra[key].toString()
}

fun LayeredMappingSpecBuilder.parchmentMappings() {
    val mappings = buildString {
        append("org.parchmentmc.data")
        append(":")
        append("parchment-$mcVersion")
        append(":")
        append(libs.versions.parchment.mappings.get())
        append("@zip")
    }

    parchment(mappings)
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
