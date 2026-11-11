package com.mikael.launcher.engine.java

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mikael.launcher.MainActivity
import com.mikael.launcher.fcl.FclGameDirectoryResolver
import com.mikael.launcher.fcl.FclModLoader
import com.mikael.launcher.fcl.FclModLoaderInstaller
import com.mikael.launcher.fcl.FclEngineBootstrap
import com.mikael.launcher.ui.screens.MinecraftInstallSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Mantém downloads/instalações ativos quando a Activity sai da tela.
 * O serviço é iniciado enquanto a Activity está visível e exibe notificação persistente.
 */
class MinecraftInstallForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager
    private var workerStarted = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service intents are serialized on the main thread. Ignore duplicate starts
        // without confusing the process-wide UI state with this service's worker state.
        if (workerStarted) return START_NOT_STICKY
        workerStarted = true
        val notification = buildNotification("Preparando instalação…", null, null, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        val request = try {
            requireNotNull(intent)
            InstallArgs(
                version = requireNotNull(intent.getStringExtra(EXTRA_VERSION)),
                metadataUrl = requireNotNull(intent.getStringExtra(EXTRA_METADATA_URL)),
                metadataSha1 = intent.getStringExtra(EXTRA_METADATA_SHA1).orEmpty(),
                loader = requireNotNull(intent.getStringExtra(EXTRA_LOADER)),
                loaderVersion = intent.getStringExtra(EXTRA_LOADER_VERSION).orEmpty(),
                forgeVersion = intent.getStringExtra(EXTRA_FORGE_VERSION).orEmpty(),
                installedProfiles = intent.getStringArrayListExtra(EXTRA_PROFILES).orEmpty()
            )
        } catch (failure: Exception) {
            MinecraftInstallSession.failExternal("Dados da instalação inválidos: ${failure.message}")
            workerStarted = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        MinecraftInstallSession.beginExternal()
        scope.launch {
            try {
                updateNotification("Instalando ${request.version} • ${request.loader}", null, null, true)
                val resultProfile = install(request)
                MinecraftInstallSession.completeExternal(resultProfile)
                updateNotification("Instalação concluída: $resultProfile", 1, 1, false)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                MinecraftInstallSession.failExternal(failure.message ?: "Falha desconhecida durante a instalação.")
                updateNotification("Falha na instalação", null, null, false)
            } finally {
                workerStarted = false
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun install(args: InstallArgs): String {
        val context = applicationContext
        val entryId = args.version
        val result = when {
            args.loader == "Forge + OptiFine" -> {
                require(args.forgeVersion.isNotBlank() && args.loaderVersion.isNotBlank()) {
                    "Selecione versões compatíveis de Forge e OptiFine."
                }
                FclModLoaderInstaller.installForgeOptiFine(context, entryId, args.forgeVersion, args.loaderVersion).profileId
            }
            args.loader in setOf("Forge", "NeoForge", "OptiFine") -> {
                require(args.loaderVersion.isNotBlank()) { "Selecione uma versão de ${args.loader}." }
                val backend = when (args.loader) {
                    "Forge" -> FclModLoader.FORGE
                    "NeoForge" -> FclModLoader.NEOFORGE
                    else -> FclModLoader.OPTIFINE
                }
                FclModLoaderInstaller.install(context, entryId, backend, args.loaderVersion).profileId
            }
            else -> {
                val gameDir = FclGameDirectoryResolver.resolve(context)
                MinecraftInstaller(gameDir).install(
                    versionId = entryId,
                    metadataUrl = args.metadataUrl,
                    metadataSha1 = args.metadataSha1
                ) { progress ->
                    MinecraftInstallSession.reportProgress(progress)
                    updateNotification(
                        "${progress.stage}: ${progress.currentFile}",
                        progress.completed,
                        progress.total,
                        true
                    )
                }
                when (args.loader) {
                    "Vanilla" -> {
                        FclEngineBootstrap.refreshAndSelectGameVersion(context, entryId)
                        entryId
                    }
                    "Fabric", "Quilt" -> {
                        require(args.loaderVersion.isNotBlank()) { "Selecione uma versão de ${args.loader}." }
                        val backend = if (args.loader == "Fabric") SupportedProfileLoader.FABRIC else SupportedProfileLoader.QUILT
                        val profile = ModLoaderProfileInstaller(gameDir).install(entryId, backend, args.loaderVersion)
                        FclEngineBootstrap.refreshAndSelectGameVersion(context, profile.profileId)
                        profile.profileId
                    }
                    else -> error("Instalador de ${args.loader} não integrado.")
                }
            }
        }
        val profiles = (args.installedProfiles + result).distinct()
        getSharedPreferences("mikael_launcher_preferences", Context.MODE_PRIVATE).edit()
            .putString("launcher_installed_version_profiles", profiles.joinToString("\n"))
            .putString("launcher_selected_version_profile", result)
            .apply()
        return result
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Instalação do Minecraft", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun buildNotification(
        title: String,
        completed: Int?,
        total: Int?,
        ongoing: Boolean
    ): Notification {
        val openLauncher = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Mikael Launcher")
            .setContentText(title)
            .setContentIntent(openLauncher)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (completed != null && total != null && total > 0) {
            builder.setProgress(total, completed.coerceIn(0, total), false)
        } else if (ongoing) {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun updateNotification(
        title: String,
        completed: Int?,
        total: Int?,
        ongoing: Boolean = true
    ) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, completed, total, ongoing))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private data class InstallArgs(
        val version: String,
        val metadataUrl: String,
        val metadataSha1: String,
        val loader: String,
        val loaderVersion: String,
        val forgeVersion: String,
        val installedProfiles: List<String>
    )

    companion object {
        private const val CHANNEL_ID = "minecraft_installation"
        private const val NOTIFICATION_ID = 7312
        private const val EXTRA_VERSION = "version"
        private const val EXTRA_METADATA_URL = "metadata_url"
        private const val EXTRA_METADATA_SHA1 = "metadata_sha1"
        private const val EXTRA_LOADER = "loader"
        private const val EXTRA_LOADER_VERSION = "loader_version"
        private const val EXTRA_FORGE_VERSION = "forge_version"
        private const val EXTRA_PROFILES = "installed_profiles"

        fun start(
            context: Context,
            minecraftVersion: String,
            metadataUrl: String,
            metadataSha1: String,
            loader: String,
            loaderVersion: String,
            forgeVersion: String,
            installedProfiles: List<String>
        ) {
            val intent = Intent(context, MinecraftInstallForegroundService::class.java).apply {
                putExtra(EXTRA_VERSION, minecraftVersion)
                putExtra(EXTRA_METADATA_URL, metadataUrl)
                putExtra(EXTRA_METADATA_SHA1, metadataSha1)
                putExtra(EXTRA_LOADER, loader)
                putExtra(EXTRA_LOADER_VERSION, loaderVersion)
                putExtra(EXTRA_FORGE_VERSION, forgeVersion)
                putStringArrayListExtra(EXTRA_PROFILES, ArrayList(installedProfiles))
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
