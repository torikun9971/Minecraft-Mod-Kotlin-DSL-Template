pluginManagement {
    repositories {
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
        }

        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }

        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
