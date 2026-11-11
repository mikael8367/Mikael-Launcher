package com.mikael.launcher.fcl

import android.content.Context
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
    NEOFORGE("neoforge", GameComponentType.NEO_FORGE)
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
 * Forge/NeoForge installers are executed by FCL's isolated :jvm process and
 * the resulting profile is saved through FCL's game repository.
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
        ensureFclReady()
        val profile = Profiles.getSelectedProfile()
        val selectedDirectory = FclGameDirectoryResolver.resolve(context.applicationContext).canonicalFile
        if (profile.gameDir.canonicalFile != selectedDirectory) {
            profile.gameDir = selectedDirectory
        }
        val dependency = profile.getDependency()
        val versionList = dependency.getVersionList(loader.componentType)
        awaitTask(versionList.loadAsync(minecraftVersion))
        val remote = versionList.getVersion(minecraftVersion, loaderVersion)
            .orElseThrow { IllegalArgumentException("A versão $loaderVersion de ${loader.patchId} não está disponível para Minecraft $minecraftVersion.") }

        val profileId = buildProfileId(minecraftVersion, loader, loaderVersion)
        require(!profile.repository.hasVersion(profileId) && !File(selectedDirectory, "versions/$profileId").exists()) {
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
