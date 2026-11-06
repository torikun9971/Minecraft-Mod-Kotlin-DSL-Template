@file:Suppress("UnstableApiUsage")

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    alias(libs.plugins.fabric.loom)
    eclipse
    idea

    `maven-publish`
    alias(libs.plugins.mod.publish.plugin)
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
        parchment(libs.parchment_mappings())
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
                programArgs(
                    "--username", prop("mc_username"),
                    "--uuid", prop("mc_uuid")
                )
            }
        }
    }
}

tasks.processResources {
    val replaceProperties = mapOf(
        "project_group" to project.group,
        "project_version" to project.version,
        "java_version" to prop("java_version"),
        "minecraft_version" to mcVersion,
        "fabric_version" to libs.versions.fabric.asProvider().get(),
        "fabric_api_version" to libs.versions.fabric.api.get(),
        "mod_id" to prop("mod_id"),
        "mod_name" to prop("mod_name"),
        "mod_version" to prop("mod_version")
    )

    inputs.properties(replaceProperties)

    filesMatching(listOf("fabric.mod.json", "pack.mcmeta")) {
        expand(replaceProperties)
    }
}

tasks.jar {
//    from("LICENSE")
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

tasks.getByName("ideaSyncTask")?.run {
    dependsOn("genSources")
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val releaseTitle = "${prop("mod_name")} ${prop("mod_version")} for ${prop("mod_loader")} $mcVersion"
//val changelogFile = file("changelog.md")

tasks.register("printReleaseVersion") {
    doLast {
        println(version)
    }
}

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
    return properties[key].toString()
}

fun prop_array(key: String): Array<String> {
    return prop_list(key).toTypedArray()
}

fun prop_list(key: String): List<String> {
    return properties[key].toString().split(",")
}

fun extra(key: String): String {
    return extra[key].toString()
}

fun LibrariesForLibs.parchment_mappings(): String {
    return buildString {
        append("org.parchmentmc.data")
        append(":")
        append("parchment-$mcVersion")
        append(":")
        append(libs.versions.parchment.mappings.get())
        append("@zip")
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
