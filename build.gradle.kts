plugins {
    /** java & neo */
    `java-library`
    alias(libs.plugins.neogradle)

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
    mavenLocal()
}

dependencies {
    implementation(libs.neoforge)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(prop("java_version"))
    }

    withSourcesJar()
}

neoForge {
    version = libs.versions.neoforge.get()

    parchment {
        mappingsVersion = libs.versions.parchment.mappings.get()
        minecraftVersion = mcVersion
    }

    /**
    accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }
    **/

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")

            logLevel = org.slf4j.event.Level.DEBUG
        }

        create("client") {
            client()

            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id"))

            if (canSpecifyUser()) {
                programArguments.addAll(
                    "--username", prop("mc_username"),
                    "--uuid", prop("mc_uuid")
                )
            }
        }

        create("server") {
            server()

            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id"))
        }

        create("gameTestServer") {
            type = "gameTestServer"

            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id"))
        }

        create("data") {
            data()

            programArguments.addAll(
                "--mod", prop("mod_id"),
                "--all",
                "--output", file("src/generated/resources/").getAbsolutePath(),
                "--existing", file("src/main/resources/").getAbsolutePath()
            )
        }
    }

    mods {
        create(prop("mod_id")) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/generated/resources")
        }
    }
}

tasks.named<Wrapper>("wrapper").configure {
    distributionType = Wrapper.DistributionType.BIN
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
            "project_group" to project.group,
            "project_version" to project.version,
            "minecraft_version" to mcVersion,
            "minecraft_version_range" to prop("minecraft_version_range"),
            "neoforge_version" to libs.versions.neoforge.get(),
            "neoforge_version_range" to prop("neoforge_version_range"),
            "loader_version_range" to prop("loader_version_range"),
            "mod_id" to prop("mod_id"),
            "mod_name" to prop("mod_name"),
            "mod_version" to prop("mod_version"),
            "mod_license" to prop("mod_license"),
//            "mod_issue" to prop("mod_issue"),
//            "mod_update_json" to prop("mod_update_json"),
//            "mod_homepage" to prop("mod_homepage"),
//            "mod_credits" to prop("mod_credits"),
            "mod_authors" to prop("mod_authors"),
            "mod_description" to prop("mod_description"),
    )

    inputs.properties(replaceProperties)

    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replaceProperties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

val releaseTitle = "${prop("mod_name")} ${prop("mod_version")} for ${prop("mod_loader")} $mcVersion"
//val changelogFile = file("changelog.md")

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = project.projectDir.resolve("repo").toURI()
        }
    }
}

/*
publishMods {
    file = modJar()
    version = "${project.version}"

    changelog = changelogFile.readText()

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

        javaVersions.add(JavaVersion.VERSION_21)
        minecraftVersions.add("1.21.1")

        client = true
        server = true

        additionalFiles.from(sourcesJar())
    }

    modrinth {
        accessToken = System.getenv("MODRINTH_TOKEN")

        projectId = "example"

        displayName = releaseTitle

        minecraftVersions.add("1.21.1")

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

fun Provider<MinimalExternalModuleDependency>.classifier(name: String): String {
    return "${this.get()}:$name"
}

fun canSpecifyUser(): Boolean {
    return hasProperty("mc_username") && hasProperty("mc_uuid")
}

fun modJar(): File {
    return tasks.jar.get().outputs.files.singleFile
}

fun sourcesJar(): File {
    return tasks.named("sourcesJar").get().outputs.files.singleFile
}
