package com.mikael.launcher.ui.screens

import android.net.Uri
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import com.mikael.launcher.engine.java.MinecraftInstaller
import com.mikael.launcher.fcl.FclGameDirectoryResolver
import com.mikael.launcher.engine.java.MinecraftInstallProgress
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.navigation.LauncherSection
import com.mikael.launcher.ui.components.AnimatedLauncherButton
import com.mikael.launcher.ui.theme.AccentBlue
import com.mikael.launcher.ui.theme.AccentPurple
import com.mikael.launcher.ui.theme.AccentYellow
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.SurfaceRaised
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary

@Composable
fun PlaceholderScreen(section: LauncherSection) {
    when (section) {
        LauncherSection.PLAY -> PlayScreen()
        LauncherSection.HORIZON_RUST -> HorizonRustScreen()
        LauncherSection.VERSIONS -> VersionsScreen()
        LauncherSection.MODS -> ModCatalogScreen()
        LauncherSection.DOWNLOADS -> DownloadsScreen()
        LauncherSection.SETTINGS -> SettingsScreen()
        LauncherSection.ACCOUNTS,
        LauncherSection.RESOURCE_PACKS,
        LauncherSection.SHADERS,
        LauncherSection.WORLDS,
        LauncherSection.PERFORMANCE,
        LauncherSection.LOGS -> AdditionalLauncherScreen(section)
        LauncherSection.HOME -> EmptySection(
            section = section,
            icon = Icons.Rounded.Gamepad,
            description = "Painel principal do launcher"
        )
    }
}

@Composable
private fun PlayScreen() {
    var engine by rememberSaveable { mutableStateOf("Minecraft Java") }
    var profile by rememberSaveable { mutableStateOf("Vanilla 1.20.1") }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    ScreenColumn(
        eyebrow = "CENTRAL DE JOGO",
        title = "Jogar",
        subtitle = "Escolha o motor e o perfil que deseja preparar."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChoiceCard(
                modifier = Modifier.weight(1f),
                title = "Minecraft Java",
                description = "Forge, Fabric, OptiFine e versões tradicionais.",
                accent = PrimaryGreen,
                selected = engine == "Minecraft Java",
                onClick = {
                    engine = "Minecraft Java"
                    profile = "Vanilla 1.20.1"
                }
            )
            ChoiceCard(
                modifier = Modifier.weight(1f),
                title = "Horizon Rust",
                description = "Motor independente. Requer compatibilidade Vulkan.",
                accent = AccentPurple,
                selected = engine == "Horizon Rust",
                onClick = {
                    engine = "Horizon Rust"
                    profile = "Horizon"
                }
            )
        }
        SectionLabel("PERFIL SELECIONADO")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Vanilla 1.20.1", "Forge 1.12.2", "Fabric 1.20.1").forEach { option ->
                FilterChip(
                    selected = profile == option && engine == "Minecraft Java",
                    onClick = {
                        engine = "Minecraft Java"
                        profile = option
                    },
                    label = { Text(option, fontSize = 11.sp) }
                )
            }
        }
        InfoCard(
            title = "Resumo da seleção",
            body = "Motor: $engine\nPerfil: $profile\nEstado: aguardando integração do motor.",
            accent = AccentBlue
        )
        Button(
            onClick = { showInfo = true },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color(0xFF062116))
        ) {
            Icon(Icons.Rounded.Gamepad, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Preparar para jogar")
        }
    }
    if (showInfo) {
        InfoDialog(
            title = "Motor ainda não conectado",
            message = "A seleção de motor e perfil já funciona. A inicialização real será ligada quando os módulos Java e Horizon forem implementados."
        ) { showInfo = false }
    }
}

private data class MinecraftCatalogVersion(
    val id: String,
    val type: String,
    val releaseTime: String,
    val url: String,
    val sha1: String
)

private const val MOJANG_VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

private suspend fun fetchMinecraftVersions(): List<MinecraftCatalogVersion> = withContext(Dispatchers.IO) {
    val connection = URL(MOJANG_VERSION_MANIFEST).openConnection()
    connection.connectTimeout = 15000
    connection.readTimeout = 20000
    connection.setRequestProperty("User-Agent", "MikaelLauncher/0.1.0")
    try {
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val versions = JSONObject(json).getJSONArray("versions")
        buildList {
            for (index in 0 until versions.length()) {
                val item = versions.getJSONObject(index)
                add(MinecraftCatalogVersion(item.getString("id"), item.getString("type"), item.optString("releaseTime"), item.getString("url"), item.optString("sha1")))
            }
        }
    } finally {
        (connection as? HttpURLConnection)?.disconnect()
    }
}

private fun isAprilFoolsVersion(id: String): Boolean {
    val normalized = id.lowercase()
    return normalized.contains("15w14") ||
        normalized.contains("20w14") ||
        normalized.contains("22w13") ||
        normalized.contains("oneblockatatime") ||
        normalized.contains("3d shareware") ||
        normalized == "2.0" ||
        normalized.startsWith("1.rv-pre") ||
        normalized.contains("23w13") ||
        normalized.contains("24w14") ||
        normalized.contains("25w14") ||
        normalized.contains("infinite") ||
        normalized.contains("potato") ||
        normalized.contains("craftmine")
}

@Composable
private fun VersionsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mikael_launcher_preferences", Context.MODE_PRIVATE) }
    var engine by rememberSaveable { mutableStateOf("Java") }
    var selectedVersion by rememberSaveable { mutableStateOf("1.12.2") }
    var selectedLoader by rememberSaveable {
        mutableStateOf(prefs.getString("launcher_selected_mod_loader", "Vanilla").orEmpty())
    }
    var lastJavaVersion by rememberSaveable { mutableStateOf("1.12.2") }
    var selectedTab by rememberSaveable { mutableStateOf("Recentes") }
    var searchText by rememberSaveable { mutableStateOf("") }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var installRequested by rememberSaveable { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf<MinecraftInstallProgress?>(null) }
    var installError by remember { mutableStateOf<String?>(null) }
    var loaderVersions by remember { mutableStateOf<List<com.mikael.launcher.engine.java.LoaderRelease>>(emptyList()) }
    var forgeVersions by remember { mutableStateOf<List<com.mikael.launcher.engine.java.LoaderRelease>>(emptyList()) }
    var selectedForgeVersion by rememberSaveable { mutableStateOf("") }
    var selectedLoaderVersion by rememberSaveable { mutableStateOf("") }
    var loaderCatalogMessage by remember { mutableStateOf("") }
    var installedVersion by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf<List<MinecraftCatalogVersion>>(emptyList()) }
    var profiles by remember {
        mutableStateOf(prefs.getString("launcher_installed_version_profiles", "").orEmpty().split("\n").filter { it.isNotBlank() })
    }
    var selectedProfile by remember {
        mutableStateOf(
            prefs.getString("launcher_selected_version_profile", "").orEmpty()
                .takeIf { selected -> profiles.contains(selected) }
                .orEmpty()
        )
    }

    LaunchedEffect(engine, retryCount) {
        if (engine == "Java" && catalog.isEmpty() && !loading) {
            loading = true
            loadError = null
            try {
                catalog = fetchMinecraftVersions()
            } catch (error: Exception) {
                loadError = error.message ?: "Não foi possível consultar o catálogo da Mojang."
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(selectedVersion, selectedLoader) {
        val fabricBackend = when (selectedLoader) {
            "Fabric" -> com.mikael.launcher.engine.java.SupportedProfileLoader.FABRIC
            "Quilt" -> com.mikael.launcher.engine.java.SupportedProfileLoader.QUILT
            else -> null
        }
        val fclBackend = when (selectedLoader) {
            "Forge" -> com.mikael.launcher.fcl.FclModLoader.FORGE
            "NeoForge" -> com.mikael.launcher.fcl.FclModLoader.NEOFORGE
            "OptiFine" -> com.mikael.launcher.fcl.FclModLoader.OPTIFINE
            "Forge + OptiFine" -> null
            else -> null
        }
        if (selectedLoader == "Forge + OptiFine") {
            loaderCatalogMessage = "Consultando versões compatíveis de Forge e OptiFine..."
            try {
                val forge = com.mikael.launcher.fcl.FclModLoaderInstaller.availableVersions(context, selectedVersion, com.mikael.launcher.fcl.FclModLoader.FORGE)
                    .map { com.mikael.launcher.engine.java.LoaderRelease(it.version, it.stable) }
                val optifine = com.mikael.launcher.fcl.FclModLoaderInstaller.availableVersions(context, selectedVersion, com.mikael.launcher.fcl.FclModLoader.OPTIFINE)
                    .map { com.mikael.launcher.engine.java.LoaderRelease(it.version, it.stable) }
                forgeVersions = forge
                loaderVersions = optifine
                selectedForgeVersion = forge.firstOrNull()?.version.orEmpty()
                selectedLoaderVersion = optifine.firstOrNull()?.version.orEmpty()
                loaderCatalogMessage = if (forge.isEmpty() || optifine.isEmpty()) {
                    "Não há versões publicadas dos dois componentes para Minecraft $selectedVersion."
                } else {
                    "Selecione uma versão de Forge e uma versão de OptiFine. O patch será aplicado nessa ordem."
                }
            } catch (error: Exception) {
                forgeVersions = emptyList()
                loaderVersions = emptyList()
                selectedForgeVersion = ""
                selectedLoaderVersion = ""
                loaderCatalogMessage = error.message ?: "Falha ao consultar os catálogos Forge/OptiFine."
            }
        } else if (fabricBackend == null && fclBackend == null) {
            loaderVersions = emptyList()
            selectedLoaderVersion = ""
            loaderCatalogMessage = if (selectedLoader == "Vanilla") "" else
                "$selectedLoader ainda não possui instalador integrado."
        } else {
            loaderCatalogMessage = "Consultando versões compatíveis..."
            try {
                val releases = if (fclBackend != null) {
                    com.mikael.launcher.fcl.FclModLoaderInstaller.availableVersions(
                        context, selectedVersion, fclBackend
                    ).map { com.mikael.launcher.engine.java.LoaderRelease(it.version, it.stable) }
                } else {
                    com.mikael.launcher.engine.java.ModLoaderProfileInstaller(
                        FclGameDirectoryResolver.resolve(context)
                    ).availableVersions(selectedVersion, requireNotNull(fabricBackend))
                }
                loaderVersions = releases
                selectedLoaderVersion = releases.firstOrNull()?.version.orEmpty()
                loaderCatalogMessage = if (releases.isEmpty()) {
                    "Nenhuma versão de $selectedLoader foi publicada para Minecraft $selectedVersion."
                } else {
                    "${releases.size} versões compatíveis encontradas."
                }
            } catch (error: Exception) {
                loaderVersions = emptyList()
                selectedLoaderVersion = ""
                loaderCatalogMessage = error.message ?: "Falha ao consultar o catálogo de $selectedLoader."
            }
        }
    }

    fun saveProfiles(updated: List<String>, selected: String) {
        profiles = updated.distinct()
        selectedProfile = selected
        prefs.edit()
            .putString("launcher_installed_version_profiles", profiles.joinToString("\n"))
            .putString("launcher_selected_version_profile", selectedProfile)
            .apply()
    }

    LaunchedEffect(installRequested) {
        if (!installRequested) return@LaunchedEffect
        installError = null
        installProgress = null
        try {
            val entry = catalog.firstOrNull { it.id == selectedVersion }
                ?: error("A versão selecionada não está disponível no catálogo oficial.")
            if (selectedLoader == "Forge + OptiFine") {
                val forgeVersion = selectedForgeVersion.takeIf { it.isNotBlank() }
                    ?: error("Selecione uma versão de Forge compatível.")
                val optiFineVersion = selectedLoaderVersion.takeIf { it.isNotBlank() }
                    ?: error("Selecione uma versão de OptiFine compatível.")
                val result = com.mikael.launcher.fcl.FclModLoaderInstaller.installForgeOptiFine(
                    context, entry.id, forgeVersion, optiFineVersion
                )
                saveProfiles(profiles + result.profileId, result.profileId)
                installedVersion = result.profileId
            } else if (selectedLoader in setOf("Forge", "NeoForge", "OptiFine")) {
                val backend = when (selectedLoader) {
                    "Forge" -> com.mikael.launcher.fcl.FclModLoader.FORGE
                    "NeoForge" -> com.mikael.launcher.fcl.FclModLoader.NEOFORGE
                    else -> com.mikael.launcher.fcl.FclModLoader.OPTIFINE
                }
                val loaderVersion = selectedLoaderVersion.takeIf { it.isNotBlank() }
                    ?: error("Não há uma versão compatível de $selectedLoader selecionada.")
                val result = com.mikael.launcher.fcl.FclModLoaderInstaller.install(
                    context, entry.id, backend, loaderVersion
                )
                saveProfiles(profiles + result.profileId, result.profileId)
                installedVersion = result.profileId
            } else {
                val gameDir = FclGameDirectoryResolver.resolve(context)
                MinecraftInstaller(gameDir).install(
                    versionId = entry.id,
                    metadataUrl = entry.url,
                    metadataSha1 = entry.sha1
                ) { progress ->
                    installProgress = progress
                }
                if (selectedLoader == "Vanilla") {
                    com.mikael.launcher.fcl.FclEngineBootstrap.refreshAndSelectGameVersion(context, entry.id)
                    saveProfiles(profiles + entry.id, entry.id)
                    installedVersion = entry.id
                } else {
                    val backend = when (selectedLoader) {
                        "Fabric" -> com.mikael.launcher.engine.java.SupportedProfileLoader.FABRIC
                        "Quilt" -> com.mikael.launcher.engine.java.SupportedProfileLoader.QUILT
                        else -> error("O instalador real de $selectedLoader ainda não está integrado.")
                    }
                    val loaderVersion = selectedLoaderVersion.takeIf { it.isNotBlank() }
                        ?: error("Não há uma versão compatível de $selectedLoader selecionada.")
                    val result = com.mikael.launcher.engine.java.ModLoaderProfileInstaller(gameDir)
                        .install(entry.id, backend, loaderVersion)
                    com.mikael.launcher.fcl.FclEngineBootstrap.refreshAndSelectGameVersion(context, result.profileId)
                    saveProfiles(profiles + result.profileId, result.profileId)
                    installedVersion = result.profileId
                }
            }
            showInfo = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            installError = error.message ?: "Falha desconhecida durante a instalação."
        } finally {
            installRequested = false
        }
    }

    val filteredVersions = remember(catalog, selectedTab, searchText) {
        catalog.filter { item ->
            val tabMatches = when (selectedTab) {
                "Recentes" -> item.type == "release"
                "Snapshots" -> item.type == "snapshot" && !isAprilFoolsVersion(item.id)
                "Beta" -> item.type == "old_beta"
                "Alpha" -> item.type == "old_alpha"
                "Primeiro de abril" -> isAprilFoolsVersion(item.id)
                else -> true
            }
            tabMatches && (searchText.isBlank() || item.id.contains(searchText.trim(), ignoreCase = true))
        }
    }

    ScreenColumn("GERENCIAMENTO", "Instalar versão", "Consulte as versões oficiais do Minecraft e organize suas instalações por categoria.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Java", "Horizon").forEach {
                FilterChip(selected = engine == it, onClick = {
                    engine = it
                    selectedVersion = if (it == "Horizon") "Horizon Rust" else lastJavaVersion
                }, label = { Text(it) })
            }
        }

        if (engine == "Java") {
            SectionLabel("CATÁLOGO OFICIAL • MOJANG")
            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier.fillMaxWidth().background(PanelSurface, RoundedCornerShape(12.dp)).padding(13.dp),
                decorationBox = { inner ->
                    Box {
                        if (searchText.isBlank()) Text("Pesquisar versão (ex.: 1.20.1 ou 24w14potato)", color = TextSecondary, fontSize = 11.sp)
                        inner()
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("Recentes", "Snapshots", "Beta", "Alpha", "Primeiro de abril", "Todas").forEach { tab ->
                    FilterChip(selected = selectedTab == tab, onClick = { selectedTab = tab }, label = { Text(tab, fontSize = 10.sp) })
                }
            }

            when {
                loading -> Text("Carregando catálogo oficial da Mojang…", color = TextSecondary, fontSize = 11.sp)
                loadError != null -> {
                    Text("Falha ao carregar: $loadError", color = AccentYellow, fontSize = 11.sp)
                    OutlinedButton(onClick = { retryCount++ }) { Text("Tentar novamente") }
                }
                catalog.isEmpty() -> Text("O catálogo ainda não foi carregado.", color = TextSecondary, fontSize = 11.sp)
                else -> {
                    Text("${filteredVersions.size} versões encontradas", color = TextSecondary, fontSize = 10.sp)
                    Column(
                        modifier = Modifier.fillMaxWidth().height(390.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filteredVersions.forEach { item ->
                            SelectableRow(
                                title = item.id,
                                subtitle = when {
                                    isAprilFoolsVersion(item.id) -> "Edição especial de primeiro de abril • ${item.type}"
                                    item.type == "release" -> "Versão estável • Release • ${item.releaseTime.take(10)}"
                                    item.type == "snapshot" -> "Versão de teste • Snapshot • ${item.releaseTime.take(10)}"
                                    item.type == "old_beta" -> "Versão histórica • Beta"
                                    item.type == "old_alpha" -> "Versão histórica • Alpha"
                                    else -> item.type
                                },
                                selected = selectedVersion == item.id,
                                onClick = {
                                    selectedVersion = item.id
                                    lastJavaVersion = item.id
                                }
                            )
                        }
                        if (filteredVersions.isEmpty()) {
                            Text("Nenhuma versão corresponde à pesquisa nesta categoria.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            InfoCard(
                title = "Horizon Rust",
                body = "O catálogo de versões do Minecraft Java não se aplica ao motor Horizon. As versões do Horizon serão fornecidas pelo próprio projeto.",
                accent = AccentPurple
            )
        }

        SectionLabel("SELEÇÃO PARA INSTALAÇÃO")
        SelectableRow(
            title = if (engine == "Java") selectedVersion else "Horizon Rust",
            subtitle = "Versão escolhida para criar um novo perfil",
            selected = true,
            onClick = { }
        )
        if (engine == "Java") {
            SectionLabel("MOD LOADER")
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf("Vanilla", "Forge", "NeoForge", "Fabric", "Legacy Fabric", "Quilt", "OptiFine", "Forge + OptiFine", "LiteLoader", "Rift").forEach { loader ->
                    FilterChip(
                        selected = selectedLoader == loader,
                        onClick = {
                            selectedLoader = loader
                            prefs.edit().putString("launcher_selected_mod_loader", loader).apply()
                        },
                        label = { Text(loader, fontSize = 10.sp) }
                    )
                }
            }
            Text(
                when (selectedLoader) {
                    "Vanilla" -> "Vanilla: instalação oficial do cliente e dependências."
                    "OptiFine" -> "OptiFine: patcher gráfico instalado pelo mecanismo FCL."
                    "Forge + OptiFine" -> loaderCatalogMessage
                    "Fabric", "Quilt", "Forge", "NeoForge" -> loaderCatalogMessage
                    else -> "$selectedLoader: instalador específico ainda não integrado. Não será exibido como instalável."
                },
                color = if (selectedLoader in setOf("Vanilla", "Fabric", "Quilt", "Forge", "NeoForge", "OptiFine", "Forge + OptiFine")) TextSecondary else AccentYellow,
                fontSize = 10.sp
            )
            if (selectedLoader == "Forge + OptiFine") {
                SectionLabel("VERSÃO DO FORGE")
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    forgeVersions.take(8).forEach { release ->
                        FilterChip(selected = selectedForgeVersion == release.version, onClick = { selectedForgeVersion = release.version }, label = { Text(release.version, fontSize = 9.sp) })
                    }
                }
                SectionLabel("VERSÃO DO OPTIFINE")
            }
            if (selectedLoader in setOf("Fabric", "Quilt", "Forge", "NeoForge", "OptiFine", "Forge + OptiFine")) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    loaderVersions.take(8).forEach { release ->
                        FilterChip(
                            selected = selectedLoaderVersion == release.version,
                            onClick = { selectedLoaderVersion = release.version },
                            label = { Text(release.version + if (release.stable) " • estável" else "", fontSize = 9.sp) }
                        )
                    }
                }
            }
            AnimatedLauncherButton(
                text = when {
                    installRequested -> "Instalando..."
                    selectedLoader == "Vanilla" -> "⬇  Baixar e instalar Vanilla"
                    selectedLoader in setOf("Fabric", "Quilt", "Forge", "NeoForge", "OptiFine", "Forge + OptiFine") -> "⬇  Instalar $selectedLoader"
                    else -> "$selectedLoader indisponível"
                },
                onClick = {
                    if (!installRequested) {
                        if (selectedLoader == "Vanilla" ||
                            selectedLoader in setOf("Fabric", "Quilt", "Forge", "NeoForge", "OptiFine") && selectedLoaderVersion.isNotBlank() || selectedLoader == "Forge + OptiFine" && selectedForgeVersion.isNotBlank() && selectedLoaderVersion.isNotBlank()
                        ) {
                            installRequested = true
                        } else {
                            installError = "$selectedLoader não possui um instalador compatível disponível para esta versão."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "O instalador base baixa o cliente, bibliotecas Java aplicáveis, índice/objetos de assets e configuração de logs. Os natives desktop não são tratados como binários Android.",
                color = TextSecondary,
                fontSize = 10.sp
            )
            installProgress?.let { progress ->
                Text(
                    "${progress.stage}: ${progress.completed}/${progress.total}\n${progress.currentFile}",
                    color = PrimaryGreen,
                    fontSize = 10.sp
                )
            }
            installError?.let { error ->
                Text("Falha na instalação: $error", color = AccentYellow, fontSize = 11.sp)
            }
        } else {
            AnimatedLauncherButton(
                text = "＋  Criar instalação",
                onClick = { showInfo = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SectionLabel("INSTALAÇÕES ADICIONADAS")
        if (profiles.isEmpty()) {
            Text("Nenhuma instalação cadastrada ainda.", color = TextSecondary, fontSize = 11.sp)
        } else {
            profiles.forEach { profile ->
                SelectableRow(
                    title = profile,
                    subtitle = if (selectedProfile == profile) "Selecionada para a tela inicial" else "Perfil cadastrado",
                    selected = selectedProfile == profile,
                    onClick = { saveProfiles(profiles, profile) }
                )
            }
            OutlinedButton(
                onClick = {
                    if (selectedProfile.isNotBlank()) {
                        saveProfiles(profiles.filterNot { it == selectedProfile }, profiles.firstOrNull { it != selectedProfile }.orEmpty())
                    }
                },
                enabled = selectedProfile.isNotBlank()
            ) { Text("Remover instalação selecionada") }
        }
    }
    if (showInfo) InfoDialog(
        "Instalação concluída",
        "O perfil $installedVersion foi instalado na pasta de jogo selecionada. A inicialização real ainda depende da conclusão e validação do fluxo de lançamento FCL."
    ) { showInfo = false }
}

@Composable
private fun ModsScreen() {
    var selectedFilter by rememberSaveable { mutableStateOf("Todos") }
    var selectedFile by rememberSaveable { mutableStateOf<String?>(null) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        selectedFile = uri?.lastPathSegment ?: uri?.toString()
    }
    ScreenColumn("PERSONALIZAÇÃO", "Mods e modpacks", "Selecione arquivos para preparar a futura biblioteca de mods.") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Todos", "Forge", "Fabric", "Modpacks").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 11.sp) }
                )
            }
        }
        InfoCard(
            title = "Biblioteca vazia",
            body = "Os mods ainda não foram importados para nenhum perfil. A seleção de categoria acima já está ativa.",
            accent = AccentPurple
        )
        OutlinedButton(onClick = {
            picker.launch(arrayOf("application/java-archive", "application/zip", "application/octet-stream"))
        }) {
            Icon(Icons.Rounded.Extension, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Selecionar arquivo .jar ou .zip")
        }
        SectionLabel("AÇÕES DO MOD SELECIONADO")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showInfo = true }) { Text("Ativar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Desativar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Configurar") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Remover") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showInfo = true }) { Text("Ver dependências") }
            OutlinedButton(onClick = { showInfo = true }) { Text("Abrir pasta") }
        }
        if (selectedFile != null) {
            SelectableRow(
                title = "Arquivo selecionado",
                subtitle = selectedFile ?: "",
                selected = true,
                onClick = { showInfo = true }
            )
            Text("O arquivo foi selecionado pelo Android, mas ainda não foi copiado para uma pasta de perfil.", color = TextSecondary, fontSize = 11.sp)
        }
    }
    if (showInfo) InfoDialog(
        "Importação ainda não concluída",
        "O seletor de arquivos está funcionando. A cópia, validação de dependências e ativação por perfil serão implementadas no módulo de mods."
    ) { showInfo = false }
}

@Composable
private fun DownloadsScreen() {
    var refreshCount by rememberSaveable { mutableIntStateOf(0) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    ScreenColumn("CENTRAL DE ARQUIVOS", "Downloads", "Acompanhe os arquivos que serão gerenciados pelo launcher.") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { refreshCount++ }) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("Atualizar")
            }
            Text("Atualizações da tela: $refreshCount", color = TextSecondary, fontSize = 11.sp)
        }
        EmptyState(
            icon = Icons.Rounded.CloudDownload,
            title = "Nenhum download em andamento",
            description = "Quando os instaladores e arquivos forem integrados, o progresso aparecerá aqui."
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDetails = true }) { Text("Pausar tudo") }
            OutlinedButton(onClick = { showDetails = true }) { Text("Retomar tudo") }
            OutlinedButton(onClick = { showDetails = true }) { Text("Cancelar fila") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDetails = true }) { Text("Limpar concluídos") }
            OutlinedButton(onClick = { showDetails = true }) { Text("Ver informações") }
        }
    }
    if (showDetails) InfoDialog(
        "Gerenciador de downloads",
        "Esta tela ainda não consulta uma fila real. O botão Atualizar atualiza o estado local da tela; downloads reais virão com o serviço de transferência."
    ) { showDetails = false }
}

@Composable
private fun SettingsScreen() {
    val settingsContext = androidx.compose.ui.platform.LocalContext.current
    var showReset by rememberSaveable { mutableStateOf(false) }
    ScreenColumn("CENTRAL DE CONTROLE", "Configurações", "Personalize o launcher, o Minecraft Java, os gráficos, os downloads e o comportamento do aplicativo.") {
        SectionLabel("APARÊNCIA E INTERFACE")
        PersistentPreferenceToggle("ui_animations", "Animações", "Transições suaves entre telas.", true)
        PersistentPreferenceToggle("ui_compact", "Cards compactos", "Reduz o espaço ocupado pelos cartões.")
        PersistentPreferenceToggle("ui_sidebar_labels", "Mostrar nomes no menu lateral", "Exibe os rótulos dos atalhos.")
        PersistentPreferenceToggle("ui_confirm_actions", "Confirmar ações importantes", "Pede confirmação antes de remover ou limpar itens.", true)
        PersistentPreferenceToggle("ui_start_home", "Abrir na tela inicial", "Sempre iniciar o launcher na página inicial.", true)

        HorizontalDivider(color = OutlineSoft)
        SectionLabel("RUNTIME JAVA E RENDERIZAÇÃO")
        RuntimeSettingsPanel()

        HorizontalDivider(color = OutlineSoft)
        SectionLabel("MINECRAFT E INSTALAÇÕES")
        PersistentPreferenceToggle("mc_auto_update", "Verificar atualizações", "Procurar novas versões do launcher e componentes.")
        PersistentPreferenceToggle("mc_keep_installers", "Manter instaladores", "Preservar arquivos baixados após a instalação.")
        PersistentPreferenceToggle("mc_isolate_profiles", "Isolar diretórios por perfil", "Separar mods, configurações e mundos por instalação.", true)
        PersistentPreferenceToggle("mc_check_mods", "Verificar compatibilidade de mods", "Avisar quando versão ou loader não corresponder.")
        PersistentPreferenceToggle("mc_backup_worlds", "Lembrar de criar backup dos mundos", "Mostrar lembrete antes de operações importantes.", true)

        HorizontalDivider(color = OutlineSoft)
        SectionLabel("DESEMPENHO E MEMÓRIA")
        PersistentPreferenceToggle("perf_auto_ram", "Gerenciamento automático de RAM", "Usar limite sugerido conforme a memória disponível.", true)
        PersistentPreferenceToggle("perf_fps", "Priorizar FPS", "Preferir configurações gráficas mais leves.")
        PersistentPreferenceToggle("perf_vsync", "Sincronização vertical", "Preferência de sincronização de quadros.")
        PersistentPreferenceToggle("perf_background", "Reduzir atividade em segundo plano", "Economizar recursos enquanto o jogo está aberto.", true)

        HorizontalDivider(color = OutlineSoft)
        SectionLabel("DOWNLOADS E REDE")
        PersistentPreferenceToggle("net_wifi_only", "Baixar somente no Wi-Fi", "Evita downloads grandes usando dados móveis.")
        PersistentPreferenceToggle("net_resume", "Retomar downloads interrompidos", "Tentar continuar transferências pausadas.", true)
        PersistentPreferenceToggle("net_verify", "Verificar arquivos baixados", "Preparar validação de tamanho e hash.", true)
        PersistentPreferenceToggle("net_notifications", "Notificações de download", "Mostrar avisos de conclusão e falha.", true)

        HorizontalDivider(color = OutlineSoft)
        SectionLabel("ARMAZENAMENTO E PRIVACIDADE")
        PersistentPreferenceToggle("storage_cleanup", "Avisar sobre arquivos temporários", "Exibir sugestões de limpeza.")
        PersistentPreferenceToggle("privacy_crash_logs", "Incluir detalhes técnicos nos relatórios", "Controla informações anexadas aos relatórios de falha.")
        PersistentPreferenceToggle("privacy_telemetry", "Estatísticas de uso opcionais", "Preferência guardada; nenhum envio está conectado.")

        HorizontalDivider(color = OutlineSoft)
        SectionLabel("ACESSIBILIDADE E DIAGNÓSTICO")
        PersistentPreferenceToggle("access_large_text", "Texto ampliado", "Preferência para melhorar a leitura.")
        PersistentPreferenceToggle("logs_verbose", "Logs detalhados", "Preparar registros mais completos para diagnóstico.")
        PersistentPreferenceToggle("logs_crash_prompt", "Perguntar antes de compartilhar logs", "Evita compartilhar relatórios sem confirmação.", true)

        HorizontalDivider(color = OutlineSoft)
        CurseForgeApiSettings()
        HorizontalDivider(color = OutlineSoft)
        InfoCard(
            title = "Sobre o Mikael Launcher",
            body = "Versão 0.1.0-alpha\nAs preferências são salvas neste aparelho. Algumas opções são preparatórias e só terão efeito quando os serviços de runtime, downloads e motores forem integrados.",
            accent = AccentYellow
        )
        OutlinedButton(onClick = { showReset = true }) { Text("Restaurar preferências") }
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Restaurar preferências?") },
            text = { Text("As preferências visuais e de funcionamento voltarão aos valores iniciais. A chave CurseForge e a runtime escolhida não serão apagadas.") },
            confirmButton = {
                TextButton(onClick = {
                    val prefs = settingsContext.getSharedPreferences(LAUNCHER_PREFS, android.content.Context.MODE_PRIVATE)
                    listOf(
                        "ui_animations", "ui_compact", "ui_sidebar_labels", "ui_confirm_actions", "ui_start_home",
                        "mc_auto_update", "mc_keep_installers", "mc_isolate_profiles", "mc_check_mods", "mc_backup_worlds",
                        "perf_auto_ram", "perf_fps", "perf_vsync", "perf_background",
                        "net_wifi_only", "net_resume", "net_verify", "net_notifications",
                        "storage_cleanup", "privacy_crash_logs", "privacy_telemetry",
                        "access_large_text", "logs_verbose", "logs_crash_prompt"
                    ).forEach { prefs.edit().remove(it).apply() }
                    showReset = false
                }) { Text("Entendi") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ScreenColumn(
    eyebrow: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(eyebrow, color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Text(title, color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
}

@Composable
private fun ChoiceCard(
    modifier: Modifier,
    title: String,
    description: String,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (selected) accent.copy(alpha = 0.13f) else PanelSurface),
        border = BorderStroke(1.dp, if (selected) accent else OutlineSoft)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selecionado", tint = accent)
            }
            Text(description, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SelectableRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (selected) PrimaryGreen.copy(alpha = 0.10f) else PanelSurface,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (selected) PrimaryGreen.copy(alpha = 0.7f) else OutlineSoft)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = TextSecondary, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, contentDescription = "Selecionado", tint = PrimaryGreen)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, accent: Color) {
    Surface(color = PanelSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OutlineSoft)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(body, color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(PanelSurface, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = TextSecondary, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Surface(color = AccentBlue.copy(alpha = 0.12f), shape = CircleShape) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.padding(18.dp).size(28.dp))
        }
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(description, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun EmptySection(section: LauncherSection, icon: ImageVector, description: String) {
    ScreenColumn("MIKAEL LAUNCHER", section.label, description) {
        EmptyState(icon, section.label, description)
    }
}

@Composable
private fun InfoDialog(title: String, message: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onClose) { Text("Entendi") } }
    )
}
