plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1"

tasks.register("chiseledBuild") {
    group = "chiseled"
    dependsOn(stonecutter.tasks.named("buildAndCollect"))
}

tasks.register("chiseledRunClient") {
    group = "chiseled"
    dependsOn(stonecutter.tasks.named("runClient"))
}

tasks.register("publishAll") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods"))
}

tasks.register("publishModrinthAll") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishModrinth"))
}

tasks.register("publishCurseforgeAll") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishCurseforge"))
}

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "rpc"

    replacements {
        string(current.parsed >= "26.1") {
            replace("classTweaker v1 named", "classTweaker v1 official")
        }
    }
}
