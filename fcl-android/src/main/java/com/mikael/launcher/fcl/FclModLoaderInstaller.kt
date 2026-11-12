package com.mikael.launcher.fcl

import android.content.Context
import com.tungsten.fcl.game.FCLGameRepository
import com.tungsten.fcl.setting.Profiles
import com.tungsten.fclcore.game.GameComponentType
import com.tungsten.fclcore.task.Task
import com.tungsten.fclcore.task.TaskExecutor
import com.tungsten.fclcore.task.TaskListener
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class FclModLoader(val patchId: String, val componentType: GameComponentType) {
    FORGE("forge", GameComponentType.FORGE),
    NEOFORGE("neoforge", GameComponentType.NEO_FORGE),
    OPTIFINE("optifine", GameComponentType.OPTIFINE),
    FORGE_OPTIFINE("forge-optifine", GameComponentType.FORGE),
    LEGACY_FABRIC("legacyfabric", GameComponentType.LEGACY_FABRIC),
    LITELOADER("liteloader", GameComponentType.LITELOADER)
}

data class FclModLoaderRelease(
    val version: String,
    val stable: Boolean
)

data class FclModLoaderInstallResult(
    val profileId: String,
    val minecraftVersion: String,
    val loader: FclModLoader,
    val loaderVersion: String
)

/**
 * Uses FCL's own component metadata, dependency graph and installer processors.
 * Forge/NeoForge use FCL's processors; OptiFine uses FCL's dedicated patcher
 * and isolated JVM processor runner. Resulting profiles are saved by FCL.
 * Legacy Fabric and LiteLoader reuse the same generic component pipeline:
 * the pinned FCL registers LegacyFabricVersionList and LiteLoaderVersionList
 * (with matching install tasks) in MojangDownloadProvider.
 */
object FclModLoaderInstaller {
    suspend fun availableVersions(
        context: Context,
        minecraftVersion: String,
        loader: FclModLoader
    ): List<FclModLoaderRelease> = withContext(Dispatchers.IO) {
        requireSafeMinecraftVersion(minecraftVersion)
        ensureFclReady(context)
        val profile = Profiles.getSelectedProfile()
        alignProfileDirectory(context, profile)
        val dependency = profile.getDependency()
        val list = dependency.getVersionList(loader.componentType)
        awaitTask(list.loadAsync(minecraftVersion))
        list.getVersions(minecraftVersion)
            .map { remote ->
                FclModLoaderRelease(
                    version = remote.getSelfVersion(),
                    stable = remote.getVersionType().name != "SNAPSHOT"
                )
            }
            .distinctBy { it.version }
    }

    suspend fun installForgeOptiFine(
        context: Context,
        minecraftVersion: String,
        forgeVersion: String,
        optiFineVersion: String
    ): FclModLoaderInstallResult = withContext(Dispatchers.IO) {
        requireSafeMinecraftVersion(minecraftVersion)
        require(forgeVersion.isNotBlank() && forgeVersion.length <= 100)
        require(optiFineVersion.isNotBlank() && optiFineVersion.length <= 100)
        ensureFclReady(context)
        val profile = Profiles.getSelectedProfile()
        val selectedDirectory = alignProfileDirectory(context, profile)
        val dependency = profile.getDependency()

        val forgeList = dependency.getVersionList(GameComponentType.FORGE)
        awaitTask(forgeList.loadAsync(minecraftVersion))
        val forgeRemote = forgeList.getVersion(minecraftVersion, forgeVersion)
            .orElseThrow { IllegalArgumentException("Forge $forgeVersion não é compatível com Minecraft $minecraftVersion.") }

        val optiFineList = dependency.getVersionList(GameComponentType.OPTIFINE)
        awaitTask(optiFineList.loadAsync(minecraftVersion))
        val optiFineRemote = optiFineList.getVersion(minecraftVersion, optiFineVersion)
            .orElseThrow { IllegalArgumentException("OptiFine $optiFineVersion não é compatível com Minecraft $minecraftVersion.") }

        val profileId = "$minecraftVersion-forge-$forgeVersion-optifine-$optiFineVersion"
            .replace(Regex("[^A-Za-z0-9_.-]"), "-")
            .take(180)
        require(FCLGameRepository.isValidVersionId(profileId)) {
            "O identificador gerado para o perfil combinado não é aceito pelo FCL."
        }
        require(!profile.repository.versionIdConflicts(profileId) && !File(selectedDirectory, "versions/$profileId").exists()) {
            "O perfil $profileId já existe."
        }

        // OptiFineInstallTask explicitly expects a Forge launch wrapper. Install Forge first,
        // then apply OptiFine to that in-memory patched version and persist only the final profile.
        // Run each FCL component task explicitly. GameBuilder.buildAsync() exposes a
        // wildcard Task type to Kotlin, so chaining it directly loses the Version type.
        // Keeping each result typed also guarantees the required Forge-before-OptiFine order.
        val baseBuildTask = dependency.gameBuilder()
            .name(profileId)
            .gameVersion(minecraftVersion)
            .buildAsync()
        awaitTask(baseBuildTask)
        val baseVersion = baseBuildTask.result as? com.tungsten.fclcore.game.Version
            ?: error("O FCL não retornou a versão-base do Minecraft.")

        val forgeTask = dependency.installLibraryAsync(baseVersion, forgeRemote)
        awaitTask(forgeTask)
        val forgePatchedVersion = forgeTask.result
            ?: error("O instalador Forge não retornou o perfil modificado.")

        val optiFineTask = dependency.installLibraryAsync(forgePatchedVersion, optiFineRemote)
        awaitTask(optiFineTask)
        val combinedVersion = optiFineTask.result
            ?: error("O instalador OptiFine não retornou o perfil combinado.")

        awaitTask(profile.repository.saveAsync(combinedVersion))

        profile.repository.refreshVersions()
        require(profile.repository.hasVersion(profileId)) {
            "O instalador terminou, mas o FCL não encontrou o perfil Forge + OptiFine salvo."
        }
        profile.selectedVersion = profileId
        Profiles.setSelectedProfile(profile)
        FclModLoaderInstallResult(profileId, minecraftVersion, FclModLoader.FORGE_OPTIFINE, "$forgeVersion + $optiFineVersion")
    }

    suspend fun install(
        context: Context,
        minecraftVersion: String,
        loader: FclModLoader,
        loaderVersion: String
    ): FclModLoaderInstallResult = withContext(Dispatchers.IO) {
        requireSafeMinecraftVersion(minecraftVersion)
        require(loaderVersion.isNotBlank() && loaderVersion.length <= 100) {
            "Versão do mod loader inválida."
        }
        ensureFclReady(context)
        val profile = Profiles.getSelectedProfile()
        val selectedDirectory = alignProfileDirectory(context, profile)
        val dependency = profile.getDependency()
        val versionList = dependency.getVersionList(loader.componentType)
        awaitTask(versionList.loadAsync(minecraftVersion))
        val remote = versionList.getVersion(minecraftVersion, loaderVersion)
            .orElseThrow { IllegalArgumentException("A versão $loaderVersion de ${loader.patchId} não está disponível para Minecraft $minecraftVersion.") }

        val profileId = buildProfileId(minecraftVersion, loader, loaderVersion)
        require(FCLGameRepository.isValidVersionId(profileId)) {
            "O identificador gerado para o perfil não é aceito pelo FCL."
        }
        require(!profile.repository.versionIdConflicts(profileId) && !File(selectedDirectory, "versions/$profileId").exists()) {
            "O perfil $profileId já existe. Escolha outra versão ou remova o perfil antigo."
        }

        val builder = dependency.gameBuilder()
            .name(profileId)
            .gameVersion(minecraftVersion)
            .version(remote)
        awaitTask(builder.buildAsync())

        profile.repository.refreshVersions()
        require(profile.repository.hasVersion(profileId)) {
            "O instalador terminou, mas o FCL não encontrou o perfil gerado no disco."
        }
        profile.selectedVersion = profileId
        Profiles.setSelectedProfile(profile)
        FclModLoaderInstallResult(profileId, minecraftVersion, loader, loaderVersion)
    }

    private fun alignProfileDirectory(
        context: Context,
        profile: com.tungsten.fcl.setting.Profile
    ): File {
        val selectedDirectory = FclGameDirectoryResolver.resolve(context.applicationContext).canonicalFile
        if (profile.gameDir.canonicalFile != selectedDirectory) {
            profile.gameDir = selectedDirectory
        }
        // Profile.gameDir starts an asynchronous repository refresh; force a synchronous
        // refresh before checking IDs or starting GameBuilder to avoid a stale version cache.
        profile.repository.refreshVersions()
        return selectedDirectory
    }

    private suspend fun ensureFclReady(context: Context) {
        if (!FclEngineBootstrap.status().initialized) {
            FclEngineBootstrap.initialize(context.applicationContext)
        }
        check(FclEngineBootstrap.status().initialized) {
            FclEngineBootstrap.status().message.ifBlank { "Não foi possível inicializar o mecanismo FCL." }
        }
    }

    private fun requireSafeMinecraftVersion(value: String) {
        require(value.isNotBlank() && value.length <= 40 && value.matches(Regex("[A-Za-z0-9_.-]+"))) {
            "Versão do Minecraft inválida."
        }
    }

    private fun buildProfileId(minecraftVersion: String, loader: FclModLoader, loaderVersion: String): String {
        val normalized = loaderVersion.replace(Regex("[^A-Za-z0-9_.-]"), "-")
            .trim('-', '.', '_')
            .take(64)
        require(normalized.isNotBlank()) { "A versão do mod loader não gerou um identificador válido." }
        return "$minecraftVersion-${loader.patchId}-$normalized"
    }

    private suspend fun awaitTask(task: Task<*>) = suspendCancellableCoroutine<Unit> { continuation ->
        val executor = task.executor(object : TaskListener() {
            override fun onStop(success: Boolean, executor: TaskExecutor) {
                if (!continuation.isActive) return
                if (success) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        executor.exception ?: IllegalStateException("A tarefa do FCL terminou sem informar o erro.")
                    )
                }
            }
        })
        continuation.invokeOnCancellation { executor.cancel() }
        try {
            executor.start()
        } catch (error: Throwable) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
}
