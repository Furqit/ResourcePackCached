plugins {
    id("dev.architectury.loom") version "1.7.+"
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
    val targets = property("mod.mc_targets").toString().split(", ")
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

        if (loader.isForge || loader.isNeoForge) {
            put("forgeConstraint", findProperty("modstoml.forge_constraint"))
        }
    }

    props.forEach(inputs::property)

    filesMatching("rpc.mixins.json") {
        expand(props)
    }
    if (loader.isFabric) {
        filesMatching("fabric.mod.json") { expand(props) }
        exclude("META-INF/mods.toml")
    }
    if (loader.isNeoForge || loader.isForge) {
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