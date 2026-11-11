package com.mikael.launcher.engine.java

/**
 * Immutable copy of the choices used by one installation. UI state may change
 * while the worker runs, so installers consume this snapshot rather than
 * reading mutable Compose state.
 */
internal data class ModLoaderInstallRequest(
    val minecraftVersion: String,
    val loader: String,
    val loaderVersion: String,
    val forgeVersion: String
)

internal fun createModLoaderInstallRequest(
    minecraftVersion: String,
    loader: String,
    loaderVersion: String,
    forgeVersion: String
): ModLoaderInstallRequest {
    require(minecraftVersion.isNotBlank() &&
        minecraftVersion.length <= 40 &&
        minecraftVersion.matches(Regex("[A-Za-z0-9_.-]+"))
    ) { "Versão do Minecraft inválida." }

    val normalizedLoader = loader.trim()
    val supportedLoaders = setOf(
        "Vanilla", "Fabric", "Quilt", "Forge", "NeoForge",
        "OptiFine", "Forge + OptiFine"
    )
    require(normalizedLoader in supportedLoaders) {
        "$normalizedLoader ainda não possui um instalador compatível integrado."
    }

    val normalizedLoaderVersion = loaderVersion.trim()
    val normalizedForgeVersion = forgeVersion.trim()
    if (normalizedLoader == "Forge + OptiFine") {
        require(normalizedForgeVersion.isNotBlank() && normalizedForgeVersion.length <= 100) {
            "Selecione uma versão de Forge compatível."
        }
        require(normalizedLoaderVersion.isNotBlank() && normalizedLoaderVersion.length <= 100) {
            "Selecione uma versão de OptiFine compatível."
        }
    } else if (normalizedLoader != "Vanilla") {
        require(normalizedLoaderVersion.isNotBlank() && normalizedLoaderVersion.length <= 100) {
            "Selecione uma versão compatível de $normalizedLoader."
        }
    }

    return ModLoaderInstallRequest(
        minecraftVersion = minecraftVersion,
        loader = normalizedLoader,
        loaderVersion = normalizedLoaderVersion,
        forgeVersion = normalizedForgeVersion
    )
}
