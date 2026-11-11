package com.mikael.launcher.engine.java

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

data class MinecraftInstallProgress(
    val stage: String,
    val completed: Int,
    val total: Int,
    val currentFile: String
) {
    val fraction: Float
        get() = if (total <= 0) 0f else completed.toFloat() / total.toFloat()
}

data class MinecraftInstallResult(
    val versionId: String,
    val gameDirectory: File,
    val completedFiles: Int
)

/**
 * Downloads official Mojang version metadata, client, applicable Maven libraries,
 * asset index/objects (including legacy aliases) and client logging configuration.
 *
 * Files are kept inside the app-private game directory. Desktop native classifiers
 * are skipped; FCL must supply Android-compatible native libraries.
 */
class MinecraftInstaller(
    private val gameDirectory: File,
    private val maxParallelDownloads: Int = 4
) {
    suspend fun install(
        versionId: String,
        metadataUrl: String,
        metadataSha1: String? = null,
        onProgress: suspend (MinecraftInstallProgress) -> Unit = {}
    ): MinecraftInstallResult = withContext(Dispatchers.IO) {
        require(versionId.isNotBlank() && !versionId.contains('/') && !versionId.contains('\\') &&
            versionId != "." && versionId != "..") { "ID de versão inválido." }
        require(metadataUrl.startsWith("https://")) { "A URL do manifesto precisa usar HTTPS." }
        require(maxParallelDownloads in 1..8) { "Paralelismo fora do limite permitido." }

        val root = gameDirectory.canonicalFile
        ensureDirectory(root, "pasta do Minecraft")
        val metadataBytes = readUrl(metadataUrl)
        if (!metadataSha1.isNullOrBlank() && sha1(metadataBytes) != metadataSha1.lowercase()) {
            error("O SHA-1 do manifesto da versão não confere.")
        }
        val metadataText = metadataBytes.toString(Charsets.UTF_8)
        val metadata = JSONObject(metadataText)
        val declaredId = metadata.optString("id", versionId)
        require(declaredId == versionId) { "O manifesto retornou uma versão diferente da selecionada." }

        val versionRoot = safeResolve(root, "versions/$versionId")
        val versionJson = safeResolve(root, "versions/$versionId/$versionId.json")
        val tasks = mutableListOf<DownloadTask>()
        val client = minecraftClientDownload(metadata)
            ?: error("Esta versão não fornece um arquivo client oficial em um formato reconhecido (downloads.client ou mainJar.downloads.artifact).")

        tasks += taskFromJson(
            client,
            safeResolve(root, "versions/$versionId/$versionId.jar"),
            "$versionId.jar"
        )

        val libraries = metadata.optJSONArray("libraries")
        if (libraries != null) {
            for (i in 0 until libraries.length()) {
                val library = libraries.optJSONObject(i) ?: continue
                if (!libraryApplies(library)) continue
                val name = library.optString("name").orEmpty()
                val artifact = minecraftLibraryArtifact(library)
                if (artifact != null) {
                    val relative = artifact.optString("path").ifBlank { mavenPath(name) }
                    tasks += taskFromJson(artifact, safeResolve(root, "libraries/$relative"), name)
                }

                // Desktop native classifiers (linux/windows/osx) are not Android ARM64
                // binaries. FCL supplies Android-compatible natives separately.

            }
        }

        val assetIndexInfo = metadata.optJSONObject("assetIndex")
        if (assetIndexInfo != null) {
            val indexId = assetIndexInfo.optString("id").ifBlank { versionId }
            val indexFile = safeResolve(root, "assets/indexes/$indexId.json")
            val indexText = readUrl(assetIndexInfo.getString("url")).toString(Charsets.UTF_8)
            val expectedIndexSha1 = assetIndexInfo.optString("sha1")
            if (expectedIndexSha1.isNotBlank() && sha1(indexText.toByteArray()) != expectedIndexSha1.lowercase()) {
                error("O índice de assets está corrompido (SHA-1 inválido).")
            }
            writeAtomically(indexFile, indexText.toByteArray(Charsets.UTF_8))
            val assetIndex = JSONObject(indexText)
            val assetEntries = minecraftAssetEntries(assetIndex)
            for (asset in assetEntries) {
                // These are desktop window decoration images. Android renders into
                // its own Surface and never calls Minecraft's desktop setWindowIcon().
                // Some legacy Mojang indexes reference icon objects that now return
                // 404; omitting only these optional images does not affect gameplay.
                if (!minecraftAssetRequiredOnAndroid(asset.name)) continue
                val prefix = asset.hash.substring(0, 2).lowercase()
                tasks += DownloadTask(
                    "https://resources.download.minecraft.net/$prefix/${asset.hash.lowercase()}",
                    safeResolve(root, "assets/objects/$prefix/${asset.hash.lowercase()}"),
                    asset.hash.lowercase(),
                    asset.size,
                    "asset ${asset.name}"
                )
            }
        }

        val logging = metadata.optJSONObject("logging")?.optJSONObject("client")?.optJSONObject("file")
        if (logging != null && logging.optString("url").isNotBlank()) {
            val path = logging.optString("id").ifBlank { "client-logging.xml" }
            tasks += taskFromJson(logging, safeResolve(root, "versions/$versionId/$path"), "logging config")
        }

        ensureDirectory(versionRoot, "pasta da versão")

        val uniqueTasks = tasks.groupBy { it.target.canonicalPath }.values.map { duplicates ->
            val hashes = duplicates.mapNotNull { it.sha1?.lowercase() }.distinct()
            val sizes = duplicates.mapNotNull { it.size }.distinct()
            require(hashes.size <= 1 && sizes.size <= 1) {
                "O manifesto contém dependências conflitantes para o mesmo arquivo."
            }
            duplicates.firstOrNull { !it.sha1.isNullOrBlank() } ?: duplicates.first()
        }
        val completed = AtomicInteger(0)
        val total = uniqueTasks.size
        val semaphore = kotlinx.coroutines.sync.Semaphore(maxParallelDownloads)
        coroutineScope {
            uniqueTasks.map { task ->
                async(Dispatchers.IO) {
                    semaphore.acquire()
                    try {
                        download(task)
                        val count = completed.incrementAndGet()
                        withContext(Dispatchers.Main) {
                            onProgress(
                                MinecraftInstallProgress(
                                    stage = "Baixando arquivos oficiais",
                                    completed = count,
                                    total = total,
                                    currentFile = task.label
                                )
                            )
                        }
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll()
        }

        // Legacy clients read assets by logical path rather than hash. Materialize
        // the virtual legacy tree (and the older resources tree when requested).
        val assetIndexForAliases = assetIndexInfo?.let {
            val indexId = it.optString("id").ifBlank { versionId }
            val indexFile = safeResolve(root, "assets/indexes/$indexId.json")
            if (indexFile.isFile) JSONObject(indexFile.readText(Charsets.UTF_8)) else null
        }
        if (assetIndexForAliases != null) {
            val entries = minecraftAssetEntries(assetIndexForAliases)
            val virtualLegacy = assetIndexForAliases.optBoolean("virtual", false)
            val mapToResources = assetIndexForAliases.optBoolean("map_to_resources", false)
            if (virtualLegacy || mapToResources) {
                for (asset in entries) {
                    if (!minecraftAssetRequiredOnAndroid(asset.name)) continue
                    val source = safeResolve(
                        root,
                        "assets/objects/${asset.hash.substring(0, 2).lowercase()}/${asset.hash.lowercase()}"
                    )
                    if (!source.isFile) error("Asset ausente após download: ${asset.name}")
                    if (virtualLegacy) {
                        val target = safeResolve(root, "assets/virtual/legacy/${asset.name}")
                        if (!isValid(target, asset.hash, asset.size)) {
                            target.parentFile?.let { ensureDirectory(it, "pasta de assets legados") }
                            source.copyTo(target, overwrite = true)
                        }
                    }
                    if (mapToResources) {
                        val target = safeResolve(root, "assets/resources/${asset.name}")
                        if (!isValid(target, asset.hash, asset.size)) {
                            target.parentFile?.let { ensureDirectory(it, "pasta de resources legados") }
                            source.copyTo(target, overwrite = true)
                        }
                    }
                }
            }
        }

        // Publish the version metadata only after every required artifact and
        // legacy alias is present. A failed/interrupted install must not look installed.
        writeAtomically(versionJson, metadataText.toByteArray(Charsets.UTF_8))

        MinecraftInstallResult(versionId, versionRoot, completed.get())
    }

    private data class DownloadTask(
        val url: String,
        val target: File,
        val sha1: String?,
        val size: Long?,
        val label: String
    )

    private fun taskFromJson(json: JSONObject, target: File, label: String): DownloadTask {
        val url = json.optString("url").trim()
        require(url.startsWith("https://")) { "URL HTTPS ausente ou inválida para $label." }
        val hash = json.optString("sha1").takeIf { it.isNotBlank() }
        require(hash == null || hash.matches(Regex("[a-fA-F0-9]{40}"))) {
            "SHA-1 inválido nos metadados de $label."
        }
        val declaredSize = if (json.has("size") && !json.isNull("size")) json.optLong("size", -1L) else -1L
        require(declaredSize >= -1L) { "Tamanho inválido nos metadados de $label." }
        return DownloadTask(url, target, hash, declaredSize.takeIf { it >= 0L }, label)
    }

    private fun libraryApplies(library: JSONObject): Boolean =
        minecraftLibraryApplies(
            library = library,
            osName = "linux",
            osArch = System.getProperty("os.arch").orEmpty(),
            osVersion = System.getProperty("os.version").orEmpty()
        )

    private fun mavenPath(coordinate: String): String = minecraftMavenPath(coordinate)

    private suspend fun download(task: DownloadTask) {
        task.target.parentFile?.let { ensureDirectory(it, "diretório ${it.path}") }
        if (task.target.isFile && isValid(task.target, task.sha1, task.size)) return
        val part = File(task.target.parentFile, task.target.name + ".part")
        if (part.exists()) part.delete()
        val connection = openHttpsConnection(task.url, "MikaelLauncher/0.1.0 (Minecraft asset installer)")
        try {
            connection.inputStream.use { input ->
                part.outputStream().buffered().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        if (!isValid(part, task.sha1, task.size)) {
            part.delete()
            error("Arquivo inválido após download: ${task.label}")
        }
        if (task.target.exists() && !task.target.delete()) {
            part.delete()
            error("Não foi possível substituir ${task.target.name}")
        }
        if (!part.renameTo(task.target)) {
            part.copyTo(task.target, overwrite = true)
            part.delete()
        }
    }

    /**
     * File.mkdirs() returns false when another parallel download creates the same
     * directory between exists() and mkdirs(). Treat that race as success only
     * after verifying that the directory now exists and is actually a directory.
     */
    private fun ensureDirectory(directory: File, description: String) {
        if (directory.isDirectory) return
        if (directory.exists()) error("O caminho de $description existe, mas não é uma pasta: ${directory.path}")
        directory.mkdirs()
        if (!directory.isDirectory) {
            error("Falha ao criar $description: ${directory.path}")
        }
    }

    private fun isValid(file: File, expectedSha1: String?, expectedSize: Long?): Boolean {
        if (!file.isFile) return false
        if (file.length() == 0L && expectedSize != 0L) return false
        if (expectedSize != null && file.length() != expectedSize) return false
        return expectedSha1.isNullOrBlank() || sha1(file) == expectedSha1.lowercase()
    }

    private fun safeResolve(root: File, relative: String): File {
        require(relative.isNotBlank() && !relative.startsWith('/') && !relative.contains('\\')) {
            "Caminho de arquivo inválido."
        }
        val target = File(root, relative).canonicalFile
        val prefix = root.canonicalPath + File.separator
        require(target.path.startsWith(prefix)) { "Caminho fora da pasta do jogo bloqueado." }
        return target
    }

    private fun readUrl(url: String): ByteArray {
        val connection = openHttpsConnection(url, "MikaelLauncher/0.1.0")
        return try {
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openHttpsConnection(initialUrl: String, userAgent: String): HttpURLConnection {
        require(initialUrl.startsWith("https://")) { "Download HTTP inseguro bloqueado." }
        var currentUrl = URL(initialUrl)
        repeat(6) {
            require(currentUrl.protocol == "https") { "Redirecionamento para HTTP inseguro bloqueado." }
            val connection = currentUrl.openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 45000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", userAgent)
            val status = try {
                connection.responseCode
            } catch (error: Exception) {
                connection.disconnect()
                throw error
            }
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                require(!location.isNullOrBlank()) { "Redirecionamento sem destino." }
                currentUrl = URL(currentUrl, location)
            } else {
                if (status !in 200..299) {
                    val failedUrl = currentUrl.toString()
                    val statusMessage = runCatching { connection.responseMessage }.getOrNull()
                    connection.disconnect()
                    error(
                        "Falha HTTP $status${statusMessage?.let { " ($it)" }.orEmpty()} " +
                            "ao baixar conteúdo oficial. URL: $failedUrl"
                    )
                }
                return connection
            }
        }
        error("O servidor excedeu o limite de redirecionamentos.")
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val parent = target.parentFile ?: error("Destino sem pasta pai.")
        ensureDirectory(parent, "pasta de destino")
        val temp = File(parent, target.name + ".tmp")
        temp.writeBytes(bytes)
        if (target.exists() && !target.delete()) error("Não foi possível atualizar ${target.name}")
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    private fun sha1(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun sha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/** Desktop-only window icons are not required by the Android Surface renderer. */
internal fun minecraftAssetRequiredOnAndroid(name: String): Boolean =
    name != "icons/icon_16x16.png" && name != "icons/icon_32x32.png"

internal fun minecraftMavenPath(coordinate: String): String {
    val normalized = coordinate.substringBefore('@')
    val parts = normalized.split(':')
    require(parts.size in 3..4 && parts.all { it.isNotBlank() }) {
        "Coordenada Maven inválida: $coordinate"
    }
    val safePart = Regex("[A-Za-z0-9_.+\\-]+")
    require(parts.all { safePart.matches(it) && it != "." && it != ".." }) {
        "Coordenada Maven contém caracteres inválidos."
    }
    val groupSegments = parts[0].split('.')
    require(groupSegments.all { it.isNotBlank() && it != "." && it != ".." }) {
        "Grupo Maven inválido."
    }
    val groupPath = groupSegments.joinToString("/")
    val artifact = parts[1]
    val version = parts[2]
    val classifier = parts.getOrNull(3)?.let { "-$it" }.orEmpty()
    val extension = coordinate.substringAfter('@', "jar")
    require(extension.matches(Regex("[A-Za-z0-9]+"))) { "Extensão Maven inválida." }
    return "$groupPath/$artifact/$version/$artifact-$version$classifier.$extension"
}


/**
 * Mojang's modern metadata uses downloads.client. Older version metadata (including
 * 1.12.2-era manifests) can expose the same client artifact under mainJar.downloads.artifact.
 */
internal fun minecraftClientDownload(metadata: JSONObject): JSONObject? =
    metadata.optJSONObject("downloads")?.optJSONObject("client")
        ?: metadata.optJSONObject("mainJar")
            ?.optJSONObject("downloads")
            ?.optJSONObject("artifact")


/**
 * Evaluates Mojang's ordered library rules. Android uses Linux-compatible Maven
 * metadata for dependency selection; native binaries still require a separate
 * Android ABI adapter before they can be loaded.
 */
internal fun minecraftLibraryApplies(
    library: JSONObject,
    osName: String,
    osArch: String,
    osVersion: String,
    features: Map<String, Boolean> = emptyMap()
): Boolean {
    val rules = library.optJSONArray("rules") ?: return true
    var allowed = false
    for (index in 0 until rules.length()) {
        val rule = rules.optJSONObject(index) ?: continue
        val os = rule.optJSONObject("os")
        val ruleOsName = os?.optString("name").orEmpty()
        val nameMatches = ruleOsName.isBlank() || ruleOsName == osName
        val archPattern = os?.optString("arch").orEmpty()
        val archMatches = archPattern.isBlank() || runCatching {
            Regex(archPattern).containsMatchIn(osArch)
        }.getOrDefault(false)
        val versionPattern = os?.optString("version").orEmpty()
        val versionMatches = versionPattern.isBlank() || runCatching {
            Regex(versionPattern).containsMatchIn(osVersion)
        }.getOrDefault(false)
        val requiredFeatures = rule.optJSONObject("features")
        val featuresMatch = requiredFeatures == null || requiredFeatures.keys().asSequence().all { key ->
            (features[key] ?: false) == requiredFeatures.optBoolean(key, false)
        }
        if (nameMatches && archMatches && versionMatches && featuresMatch) {
            allowed = rule.optString("action") == "allow"
        }
    }
    return allowed
}

/** Resolves a Maven artifact even when metadata omits downloads.artifact. */
internal fun minecraftLibraryArtifact(library: JSONObject): JSONObject? {
    val coordinate = library.optString("name").trim()
    if (coordinate.isBlank()) return null
    val artifact = library.optJSONObject("downloads")?.optJSONObject("artifact")
    val path = artifact?.optString("path").orEmpty().ifBlank { minecraftMavenPath(coordinate) }
    val explicitUrl = artifact?.optString("url").orEmpty().trim()
    val repository = library.optString("url").trim().ifBlank { "https://libraries.minecraft.net/" }
    val resolvedUrl = explicitUrl.ifBlank { repository.trimEnd('/') + "/" + path }
    require(resolvedUrl.startsWith("https://")) { "Repositório HTTPS inválido para a biblioteca $coordinate." }
    return JSONObject()
        .put("url", resolvedUrl)
        .put("path", path)
        .put("sha1", artifact?.optString("sha1").orEmpty())
        .put("size", artifact?.optLong("size", -1L) ?: -1L)
}


internal data class MinecraftAssetEntry(
    val name: String,
    val hash: String,
    val size: Long
)

/**
 * Validates the asset index instead of silently skipping malformed records.
 * Legacy virtual/resource aliases are materialized after their hash objects exist.
 */
internal fun minecraftAssetEntries(index: JSONObject): List<MinecraftAssetEntry> {
    val objects = index.optJSONObject("objects")
        ?: throw IllegalArgumentException("Índice de assets incompatível: campo objects ausente.")
    val entries = mutableListOf<MinecraftAssetEntry>()
    val keys = objects.keys()
    while (keys.hasNext()) {
        val name = keys.next()
        require(name.isNotBlank() && !name.startsWith('/') && !name.contains('\\')) {
            "Nome de asset inválido: $name"
        }
        val segments = name.split('/')
        require(segments.all { it.isNotBlank() && it != "." && it != ".." }) {
            "Caminho de asset inseguro: $name"
        }
        val value = objects.optJSONObject(name)
            ?: throw IllegalArgumentException("Registro de asset inválido: $name")
        val hash = value.optString("hash").lowercase()
        require(hash.matches(Regex("[a-f0-9]{40}"))) {
            "Hash SHA-1 inválido no asset: $name"
        }
        val size = value.optLong("size", -1L)
        require(size >= 0L) { "Tamanho inválido no asset: $name" }
        entries += MinecraftAssetEntry(name, hash, size)
    }
    return entries
}
