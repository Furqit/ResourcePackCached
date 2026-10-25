pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.+"
    id("dev.kikugie.loom-back-compat") version "0.3"
}

stonecutter {
    create(rootProject) {
        versions("1.20.4", "26.1")
        version("1.21.1-neoforge", "1.21.1")
        version("26.3-neoforge", "26.3")
        vcsVersion = "26.1"
    }
}

rootProject.name = "ResourcePackCached"
