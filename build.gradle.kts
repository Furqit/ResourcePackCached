import java.util.*

plugins {
    id("dev.architectury.loom") version "1.7.+"
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
    val mixins = property("mod.mixins")
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

stonecutter.const("fabric", loader.isFabric)
stonecutter.const("neoforge", loader.isNeoForge)
stonecutter.const("forge", loader.isForge)

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
        put("mixins", mod.mixins)
        put("loader", loader.loader)

        if (loader.isForge) {
            put("forgeConstraint", "[${findProperty("deps.forge")},)")
        }
        if (loader.isNeoForge) {
            put("forgeConstraint", "[${findProperty("deps.neoforge")},)")
        }
    }

    props.forEach(inputs::property)

    filesMatching("rpc.mixins.json") {
        expand(props)
    }
    if (loader.isFabric) {
        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
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
    if (loader.isFabric) {
        exclude("pack.mcmeta")
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

@Suppress("TYPE_MISMATCH", "UNRESOLVED_REFERENCE")
fun <T> optionalProp(property: String, block: (String) -> T?): T? =
    findProperty(property)?.toString()?.takeUnless { it.isBlank() }?.let(block)

fun isPropDefined(property: String): Boolean {
    return property(property)?.toString()?.isNotBlank() ?: false
}

publishMods {
    file = tasks.remapJar.get().archiveFile
    displayName = "${mod.name} ${mod.version}"
    version = "${mod.version}"
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add(loader.loader)

    val versions = listOf("1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7")
        .filter { ver ->
            mc.dep.toString().split(" ")
                .all { constraint ->
                    when {
                        constraint.startsWith(">=") -> stonecutter.compare(ver, constraint.substring(2)) >= 0
                        constraint.startsWith("<=") -> stonecutter.compare(ver, constraint.substring(2)) <= 0
                        else -> true
                    }
                }
        }

    modrinth {
        projectId = "d4phKsx2"
        accessToken = localProperties.getProperty("MODRINTH_TOKEN")
        versions.forEach { minecraftVersions.add(it) }
    }

    curseforge {
        projectId = "1125284"
        accessToken = localProperties.getProperty("CURSEFORGE_TOKEN")
        versions.forEach { minecraftVersions.add(it) }
    }
}