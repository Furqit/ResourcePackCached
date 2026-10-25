import dev.kikugie.loomx.LoomCompatDependencyExtension
import dev.kikugie.loomx.LoomCompatProjectExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import java.util.*

plugins {
    id("dev.kikugie.loom-back-compat") apply false
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.+"
}

val neoLoader = sc.current.project.endsWith("-neoforge")
plugins.apply(if (neoLoader) "net.neoforged.moddev" else "dev.kikugie.loom-back-compat")

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.let {
        load(it.inputStream())
    }
}

base.archivesName = property("mod.id") as String
val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")?.asList().orEmpty().map { it.toString() }
version = "${property("mod.version")}+${compatibleVersions.first()}${if (compatibleVersions.size > 1) "-${compatibleVersions.last()}" else ""}${if (neoLoader) "-neoforge" else "-fabric"}"

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_21
}

if (neoLoader) {
    repositories {
        maven("https://maven.neoforged.net/releases/")
    }

    configure<NeoForgeExtension> {
        version = property("deps.neo_loader") as String

        mods {
            register(property("mod.id") as String) {
                sourceSet(sourceSets.main.get())
            }
        }

        runs {
            register("client") {
                gameDirectory = file("../../run/")
                client()
            }
        }
    }
} else {
    dependencies {
        add("minecraft", "com.mojang:minecraft:${sc.current.version}")
        (dependencies as ExtensionAware).the<LoomCompatDependencyExtension>().applyMojangMappings()
        add("modImplementation", "net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

        add("testImplementation", platform("org.junit:junit-bom:5.13.4"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        add("testImplementation", "net.fabricmc:fabric-loader-junit:${property("deps.fabric_loader")}")
    }

    configure<LoomGradleExtensionAPI> {
        fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
        accessWidenerPath = sc.process(
            rootProject.file("src/main/resources/rpc.ct"),
            "build/processed.ct"
        )

        decompilerOptions.named("vineflower") {
            options.put("mark-corresponding-synthetics", "1")
        }

        runConfigs.all {
            vmArgs("-Dmixin.debug.export=true")
            runDir = "../../run"
        }
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    if (neoLoader) register("publishModsNeoforge") {
        group = "publishing"
        dependsOn("publishMods")
    } else register("publishModsFabric") {
        group = "publishing"
        dependsOn("publishMods")
    }

    test {
        useJUnitPlatform()
        testLogging.events("passed", "skipped", "failed")
        if (neoLoader) enabled = false
    }

    if (neoLoader) compileTestJava {
        enabled = false
    }

    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
            if (neoLoader) {
                register("neo_version", "deps.neo_loader")
                set(
                    "loader_header",
                    if (sc.current.parsed >= "26.1") "" else "modLoader=\"javafml\"\nloaderVersion=\"[1,)\""
                )
            }
        }

        if (neoLoader) filesMatching("META-INF/neoforge.mods.toml") { expand(props) } else filesMatching("fabric.mod.json") {
            expand(
                props
            )
        }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        if (neoLoader) exclude("fabric.mod.json", "rpc.ct") else exclude("META-INF/neoforge.mods.toml", "META-INF/accesstransformer.cfg")
    }

    if (neoLoader) named<Task>("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        if (neoLoader) from(jar.flatMap { it.archiveFile }) else from(project.the<LoomCompatProjectExtension>().modJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

val builtJar =
    if (neoLoader) project.tasks.jar.flatMap { it.archiveFile } else project.the<LoomCompatProjectExtension>().modJar.flatMap { it.archiveFile }

val modLoaderName = if (neoLoader) "neoforge" else "fabric"

publishMods {
    file.set(builtJar)
    displayName = "${property("mod.name")} ${property("mod.version")}-$modLoaderName"
    version = "${property("mod.version")}-$modLoaderName"
    changelog = rootProject.file("CHANGELOG.md").readText()
    type = STABLE
    modLoaders.add(modLoaderName)

    modrinth {
        projectId = "d4phKsx2"
        accessToken = localProperties.getProperty("MODRINTH_TOKEN")
        minecraftVersionRange {
            start = compatibleVersions.first()
            end = compatibleVersions.last()
        }
    }

    curseforge {
        projectId = "1125284"
        projectSlug = "resourcepackcached"
        accessToken = localProperties.getProperty("CURSEFORGE_TOKEN")
        client = true
        server = false
        minecraftVersionRange {
            start = compatibleVersions.first()
            end = compatibleVersions.last()
        }
    }
}
