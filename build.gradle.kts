import java.util.*

plugins {
    id("dev.architectury.loom") version "1.11.+"
    id("me.modmuss50.mod-publish-plugin") version "0.8.+"
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.let {
        load(it.inputStream())
    }
}

class ModData {
    val id = property("mod.id").toString()
    val name = property("mod.name")
    val version = property("mod.version")
    val group = property("mod.group").toString()
    val description = property("mod.description")
    val source = property("mod.source")
    val issues = property("mod.issues")
    val license = property("mod.license").toString()
    val modrinth = property("mod.modrinth")
    val curseforge = property("mod.curseforge")
    val discord = property("mod.discord")
}

class LoaderData {
    val loader = loom.platform.get().name.lowercase()
    val isFabric = loader == "fabric"
    val isNeoForge = loader == "neoforge"
    val isForge = loader == "forge"
}

class McData {
    val version = property("mod.mc_version")
    val dep = property("mod.mc_dep")
}

val mc = McData()
val mod = ModData()
val loader = LoaderData()

val mcVersion = stonecutter.current.project.substringBeforeLast('-')
version = "${mod.version}+${mc.version}-${loader.loader}"
group = mod.group
base { archivesName.set(mod.id) }

stonecutter.constants["fabric"] = loader.isFabric
stonecutter.constants["neoforge"] = loader.isNeoForge
stonecutter.constants["forge"] = loader.isForge

loom {
    silentMojangMappingsLicense()

    runConfigs.all {
        ideConfigGenerated(stonecutter.current.isActive)
        runDir = "../../run"
    }
    if (loader.isForge) {
        forge {
            mixinConfig("rpc.mixins.json")
        }
    }

    runConfigs.remove(runConfigs["server"])
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${mcVersion}")

    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
    })

    if (loader.isFabric) {
        modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric")}")
    } else if (loader.isNeoForge) {
        "neoForge"("net.neoforged:neoforge:${findProperty("deps.neoforge")}")
    } else if (loader.isForge) {
        "forge"("net.minecraftforge:forge:${findProperty("deps.forge")}")
    }
}

java {
    val java = JavaVersion.VERSION_17
    sourceCompatibility = java
    targetCompatibility = java
}

tasks.processResources {
    val props = buildMap {
        put("id", mod.id)
        put("name", mod.name)
        put("version", mod.version)
        put("mcdep", mc.dep)
        put("description", mod.description)
        put("source", mod.source)
        put("issues", mod.issues)
        put("license", mod.license)
        put("modrinth", mod.modrinth)
        put("curseforge", mod.curseforge)
        put("discord", mod.discord)
        put("loader", loader.loader)

        if (loader.isForge) {
            put("forgeConstraint", "[${findProperty("deps.forge")},)")
        }
        if (loader.isNeoForge) {
            put("forgeConstraint", "[${findProperty("deps.neoforge")},)")
        }
    }

    props.forEach(inputs::property)

    if (loader.isFabric) {
        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta")
    }
    if (loader.isNeoForge) {
        filesMatching("META-INF/mods.toml") { expand(props) }
        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
        exclude("fabric.mod.json")
    }
    if (loader.isForge) {
        filesMatching("META-INF/mods.toml") { expand(props) }
        exclude("fabric.mod.json")
    }
}

if (stonecutter.current.isActive) {
    rootProject.tasks.register("buildActive") {
        group = "project"
        dependsOn(tasks.named("build"))
    }
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}"))
    dependsOn("build")
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    displayName = "${mod.name} ${mod.version}"
    version = "${mod.version}"
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add(loader.loader)
    val dep = mc.dep.toString()
    val lower = """>=([0-9.]+)""".toRegex().find(dep)?.groupValues?.get(1)
    val upper = """><=([0-9.]+)""".toRegex().find(dep)?.groupValues?.get(1)

    modrinth {
        projectId = "d4phKsx2"
        accessToken = localProperties.getProperty("MODRINTH_TOKEN")
        minecraftVersionRange {
            start = lower ?: "latest"
            end = upper ?: "latest"
        }
        announcementTitle = "Download from Modrinth"
    }

    curseforge {
        projectId = "1125284"
        projectSlug = "resourcepackcached"
        accessToken = localProperties.getProperty("CURSEFORGE_TOKEN")
        minecraftVersionRange {
            start = lower ?: "latest"
            end = upper ?: "latest"
        }
        announcementTitle = "Download from CurseForge"
    }
}