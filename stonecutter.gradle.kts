plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.4-fabric" /* [SC] DO NOT EDIT */

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

stonecutter.tasks {
    order("publishMods")
}
