plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.20.4-fabric" /* [SC] DO NOT EDIT */

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "chiseled"
    ofTask("buildAndCollect")
}

stonecutter registerChiseled tasks.register("chiseledRunClient", stonecutter.chiseled) {
    group = "chiseled"
    ofTask("runClient")
}

stonecutter registerChiseled tasks.register("chiseledPublishMods", stonecutter.chiseled) {
    group = "chiseled"
    ofTask("publishMods")
}
