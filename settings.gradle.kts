pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.architectury.dev")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
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
        mc("1.20.4", listOf("fabric"))
        mc("1.20.6", listOf("neoforge"))
        vcsVersion = "1.20.4-fabric"
    }
}

rootProject.name = "ResourcePackCached"
