pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.architectury.dev")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.+"
}

stonecutter {
    create(rootProject) {
        fun mc(mcVersion: String, loaders: Iterable<String>) {
            for (loader in loaders) {
                version("$mcVersion-$loader", mcVersion)
            }
        }

        mc("1.20.1", listOf("fabric", "forge"))
        mc("1.20.2", listOf("fabric", "forge", "neoforge"))
        mc("1.20.4", listOf("fabric", "forge", "neoforge"))
        vcsVersion = "1.20.4-fabric"
    }
}

rootProject.name = "ResourcePackCached"
