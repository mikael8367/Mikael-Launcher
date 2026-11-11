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
    val downloadedFiles: Int
)

/**
 * Downloads official Mojang version metadata, client, applicable Maven libraries,
 * native classifier archives, asset index/objects and client logging configuration.
 *
 * Files are kept inside the app-private game directory. Native archives are stored
 * but deliberately not extracted/advertised as Android-compatible until the FCL
 * ARM64 native adapter is wired and verified.
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
        if (root.exists()) {
            require(root.isDirectory) { "O caminho de dados do Minecraft não é uma pasta." }
        } else if (!root.mkdirs()) {
            error("Não foi possível criar a pasta do Minecraft.")
        }
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
        val downloads = metadata.optJSONObject("downloads")
        val client = downloads?.optJSONObject("client")
            ?: error("Esta versão não fornece um arquivo client oficial para instalação automática.")

        tasks += DownloadTask(
            client.getString("url"),
            safeResolve(root, "versions/$versionId/$versionId.jar"),
            client.optString("sha1").takeIf { it.isNotBlank() },
            client.optLong("size").takeIf { it > 0L },
            "$versionId.jar"
        )

        val libraries = metadata.optJSONArray("libraries")
        if (libraries != null) {
            for (i in 0 until libraries.length()) {
                val library = libraries.optJSONObject(i) ?: continue
                if (!libraryApplies(library)) continue
                val name = library.optString("name")
                val libDownloads = library.optJSONObject("downloads")
                val artifact = libDownloads?.optJSONObject("artifact")
                if (artifact != null) {
                    val relative = artifact.optString("path").ifBlank { mavenPath(name) }
                    tasks += taskFromJson(artifact, safeResolve(root, "libraries/$relative"), name)
                } else {
                    val coordinate = name.takeIf { it.isNotBlank() } ?: continue
                    val relative = mavenPath(coordinate)
                    val repository = library.optString("url").ifBlank { "https://libraries.minecraft.net/" }
                    tasks += DownloadTask(
                        repository.trimEnd('/') + "/" + relative,
                        safeResolve(root, "libraries/$relative"),
                        null,
                        null,
                        coordinate
                    )
                }

                val natives = library.optJSONObject("natives")
                val classifiers = libDownloads?.optJSONObject("classifiers")
                val nativeTemplate = natives?.optString("linux")?.takeIf { it.isNotBlank() }
                if (nativeTemplate != null && classifiers != null) {
                    val arch = if (System.getProperty("os.arch").contains("64")) "64" else "32"
                    val classifier = nativeTemplate.replace("$" + "{arch}", arch)
                    val nativeArtifact = classifiers.optJSONObject(classifier)
                    if (nativeArtifact != null) {
                        val relative = nativeArtifact.optString("path").ifBlank {
                            mavenPath(name.substringBefore('@') + ":" + classifier)
                        }
                        tasks += taskFromJson(
                            nativeArtifact,
                            safeResolve(root, "libraries/$relative"),
                            "$name ($classifier)"
                        )
                    }
                }
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
            val objects = JSONObject(indexText).optJSONObject("objects")
            if (objects != null) {
                val keys = objects.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = objects.optJSONObject(key) ?: continue
                    val hash = obj.optString("hash")
                    if (!hash.matches(Regex("[a-fA-F0-9]{40}"))) continue
                    val prefix = hash.substring(0, 2).lowercase()
                    tasks += DownloadTask(
                        "https://resources.download.minecraft.net/$prefix/$hash",
                        safeResolve(root, "assets/objects/$prefix/$hash"),
                        hash.lowercase(),
                        obj.optLong("size").takeIf { it > 0L },
                        "asset $key"
                    )
                }
            }
        }

        val logging = metadata.optJSONObject("logging")?.optJSONObject("client")?.optJSONObject("file")
        if (logging != null && logging.optString("url").isNotBlank()) {
            val path = logging.optString("id").ifBlank { "client-logging.xml" }
            tasks += taskFromJson(logging, safeResolve(root, "versions/$versionId/$path"), "logging config")
        }

        if (!versionRoot.exists() && !versionRoot.mkdirs()) {
            error("Não foi possível criar a pasta da versão.")
        }
        writeAtomically(versionJson, metadataText.toByteArray(Charsets.UTF_8))

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

        MinecraftInstallResult(versionId, versionRoot, completed.get())
    }

    private data class DownloadTask(
        val url: String,
        val target: File,
        val sha1: String?,
        val size: Long?,
        val label: String
    )

    private fun taskFromJson(json: JSONObject, target: File, label: String) = DownloadTask(
        json.getString("url"),
        target,
        json.optString("sha1").takeIf { it.isNotBlank() },
        json.optLong("size").takeIf { it > 0L },
        label
    )

    private fun libraryApplies(library: JSONObject): Boolean {
        val rules = library.optJSONArray("rules") ?: return true
        var allowed = false
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val os = rule.optJSONObject("os")
            val osName = os?.optString("name")
            val osMatches = osName.isNullOrBlank() || osName == "linux"
            val archPattern = os?.optString("arch").orEmpty()
            val archMatches = archPattern.isBlank() || runCatching {
                Regex(archPattern).containsMatchIn(System.getProperty("os.arch").orEmpty())
            }.getOrDefault(false)
            val features = rule.optJSONObject("features")
            val featuresMatch = features == null || features.keys().asSequence().all { key ->
                !features.optBoolean(key, false)
            }
            if (osMatches && archMatches && featuresMatch) {
                allowed = rule.optString("action") == "allow"
            }
        }
        return allowed
    }

    private fun mavenPath(coordinate: String): String = minecraftMavenPath(coordinate)

    private suspend fun download(task: DownloadTask) {
        task.target.parentFile?.let { if (!it.exists() && !it.mkdirs()) error("Falha ao criar ${it.path}") }
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

    private fun isValid(file: File, expectedSha1: String?, expectedSize: Long?): Boolean {
        if (!file.isFile || file.length() == 0L) return false
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
                    connection.disconnect()
                    error("Falha HTTP $status ao baixar conteúdo oficial.")
                }
                return connection
            }
        }
        error("O servidor excedeu o limite de redirecionamentos.")
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val parent = target.parentFile ?: error("Destino sem pasta pai.")
        if (!parent.exists() && !parent.mkdirs()) error("Não foi possível criar ${parent.path}")
        require(parent.isDirectory) { "A pasta de destino não é válida." }
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
