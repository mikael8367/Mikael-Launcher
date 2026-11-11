package com.mikael.launcher.engine.java

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class SupportedProfileLoader(val displayName: String, val metadataBase: String) {
    FABRIC("Fabric", "https://meta.fabricmc.net/v2"),
    QUILT("Quilt", "https://meta.quiltmc.org/v3")
}

data class LoaderRelease(
    val version: String,
    val stable: Boolean
)

data class LoaderInstallResult(
    val profileId: String,
    val loader: SupportedProfileLoader,
    val loaderVersion: String,
    val installedLibraries: Int,
    val profileFile: File
)

/**
 * Installs launcher-compatible Fabric and Quilt client profiles.
 *
 * This does not execute desktop installer JARs. It consumes the official
 * metadata APIs, downloads profile libraries, and creates an inheriting
 * version JSON. Forge/NeoForge processors and legacy patch-based loaders
 * require separate backends and are intentionally not faked here.
 */
class ModLoaderProfileInstaller(
    private val gameDirectory: File
) {
    suspend fun availableVersions(
        minecraftVersion: String,
        loader: SupportedProfileLoader
    ): List<LoaderRelease> = withContext(Dispatchers.IO) {
        requireSafeId(minecraftVersion)
        val endpoint = when (loader) {
            SupportedProfileLoader.FABRIC -> "${loader.metadataBase}/versions/loader/$minecraftVersion"
            SupportedProfileLoader.QUILT -> "${loader.metadataBase}/versions/loader/$minecraftVersion"
        }
        val array = JSONArray(readHttps(endpoint))
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val version = item.optJSONObject("loader")?.optString("version").orEmpty()
                if (version.isNotBlank()) {
                    add(LoaderRelease(version, item.optJSONObject("loader")?.optBoolean("stable", false) ?: false))
                }
            }
        }
    }

    suspend fun install(
        minecraftVersion: String,
        loader: SupportedProfileLoader,
        loaderVersion: String
    ): LoaderInstallResult = withContext(Dispatchers.IO) {
        requireSafeId(minecraftVersion)
        requireSafeId(loaderVersion)
        val root = gameDirectory.canonicalFile
        require(root.isDirectory) { "Instale primeiro o Minecraft $minecraftVersion." }
        val baseFile = safeResolve(root, "versions/$minecraftVersion/$minecraftVersion.json")
        require(baseFile.isFile) { "O perfil base de Minecraft $minecraftVersion não foi instalado." }
        val base = JSONObject(baseFile.readText(Charsets.UTF_8))
        require(base.optString("id") == minecraftVersion) { "O manifesto base não corresponde à versão selecionada." }

        val profileUrl = "${loader.metadataBase}/versions/loader/$minecraftVersion/$loaderVersion/profile/json"
        val profile = JSONObject(readHttps(profileUrl))
        val parent = profile.optString("inheritsFrom").ifBlank { minecraftVersion }
        require(parent == minecraftVersion) { "O perfil do mod loader aponta para outra versão do Minecraft." }
        val profileId = profile.optString("id").ifBlank { "${loader.name.lowercase()}-$loaderVersion-$minecraftVersion" }
        requireSafeId(profileId)

        val profileLibraries = profile.optJSONArray("libraries") ?: JSONArray()
        var installedCount = 0
        for (i in 0 until profileLibraries.length()) {
            val library = profileLibraries.optJSONObject(i) ?: continue
            val artifact = resolveLibrary(library) ?: continue
            val path = artifact.getString("path")
            val target = safeResolve(root, "libraries/$path")
            downloadArtifact(artifact.getString("url"), target, artifact.optString("sha1").takeIf { it.isNotBlank() }, artifact.optLong("size", -1L).takeIf { it >= 0L })
            installedCount++
        }

        val merged = mergeLoaderProfile(base, profile, profileId, minecraftVersion)
        val profileDir = safeResolve(root, "versions/$profileId")
        if (!profileDir.exists() && !profileDir.mkdirs()) error("Não foi possível criar a pasta do perfil $profileId.")
        val targetProfile = File(profileDir, "$profileId.json")
        writeAtomically(targetProfile, merged.toString(2).toByteArray(Charsets.UTF_8))

        LoaderInstallResult(profileId, loader, loaderVersion, installedCount, targetProfile)
    }

    private fun resolveLibrary(library: JSONObject): JSONObject? {
        val coordinate = library.optString("name").trim()
        if (coordinate.isBlank()) return null
        val artifact = library.optJSONObject("downloads")?.optJSONObject("artifact")
        val path = artifact?.optString("path").orEmpty().ifBlank { minecraftMavenPath(coordinate) }
        requireSafeRelativePath(path)
        val explicitUrl = artifact?.optString("url").orEmpty().trim()
        val repository = library.optString("url").trim().ifBlank { "https://maven.fabricmc.net/" }
        val url = explicitUrl.ifBlank { repository.trimEnd('/') + "/" + path }
        require(url.startsWith("https://")) { "Repositório inseguro para a biblioteca $coordinate." }
        val hash = artifact?.optString("sha1").orEmpty()
        require(hash.isBlank() || hash.matches(Regex("[a-fA-F0-9]{40}"))) { "SHA-1 inválido para $coordinate." }
        val size = artifact?.optLong("size", -1L) ?: -1L
        require(size >= -1L) { "Tamanho inválido para $coordinate." }
        return JSONObject().put("path", path).put("url", url).put("sha1", hash).put("size", size)
    }

    private fun downloadArtifact(url: String, target: File, expectedSha1: String?, expectedSize: Long?) {
        target.parentFile?.let { if (!it.exists() && !it.mkdirs()) error("Falha ao criar pasta de biblioteca.") }
        if (target.isFile && valid(target, expectedSha1, expectedSize)) return
        val part = File(target.parentFile, target.name + ".part")
        if (part.exists()) part.delete()
        val connection = openHttps(url)
        try {
            connection.inputStream.use { input ->
                part.outputStream().buffered().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }
        if (!valid(part, expectedSha1, expectedSize)) {
            part.delete()
            error("Biblioteca inválida após download: ${target.name}")
        }
        if (target.exists() && !target.delete()) {
            part.delete()
            error("Não foi possível substituir ${target.name}.")
        }
        if (!part.renameTo(target)) {
            part.copyTo(target, overwrite = true)
            part.delete()
        }
    }

    private fun valid(file: File, sha1: String?, size: Long?): Boolean {
        if (!file.isFile || file.length() == 0L && size != 0L) return false
        if (size != null && file.length() != size) return false
        return sha1.isNullOrBlank() || sha1(file) == sha1.lowercase()
    }

    private fun readHttps(url: String): String {
        val connection = openHttps(url)
        return try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openHttps(initial: String): HttpURLConnection {
        var current = URL(initial)
        repeat(6) {
            require(current.protocol == "https") { "Redirecionamento HTTP inseguro bloqueado." }
            val connection = current.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("User-Agent", "MikaelLauncher/0.1.0")
            val status = connection.responseCode
            if (status in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                require(!location.isNullOrBlank()) { "Redirecionamento sem destino." }
                current = URL(current, location)
            } else {
                if (status !in 200..299) {
                    connection.disconnect()
                    error("Falha HTTP $status ao consultar ou baixar metadados do mod loader.")
                }
                return connection
            }
        }
        error("Limite de redirecionamentos excedido.")
    }

    private fun safeResolve(root: File, relative: String): File {
        requireSafeRelativePath(relative)
        val target = File(root, relative).canonicalFile
        require(target.path.startsWith(root.canonicalPath + File.separator)) {
            "Caminho fora da pasta do jogo bloqueado."
        }
        return target
    }

    private fun requireSafeId(value: String) {
        require(value.isNotBlank() && value.matches(Regex("[A-Za-z0-9_.+ -]+")) && value != "." && value != "..") {
            "Identificador de versão inválido."
        }
    }

    private fun requireSafeRelativePath(path: String) {
        require(path.isNotBlank() && !path.startsWith('/') && !path.contains('\\') &&
            path.split('/').none { it.isBlank() || it == "." || it == ".." }) {
            "Caminho de biblioteca inválido."
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeBytes(bytes)
        if (target.exists() && !target.delete()) error("Não foi possível substituir o perfil.")
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

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

internal fun mergeLoaderProfile(
    base: JSONObject,
    loaderProfile: JSONObject,
    profileId: String,
    minecraftVersion: String
): JSONObject {
    val merged = JSONObject(base.toString())
    val libraries = JSONArray()
    val seen = linkedSetOf<String>()
    fun append(source: JSONArray?) {
        if (source == null) return
        for (i in 0 until source.length()) {
            val item = source.optJSONObject(i) ?: continue
            val name = item.optString("name")
            if (name.isNotBlank() && seen.add(name)) libraries.put(JSONObject(item.toString()))
        }
    }
    append(base.optJSONArray("libraries"))
    append(loaderProfile.optJSONArray("libraries"))
    merged.put("id", profileId)
        .put("inheritsFrom", minecraftVersion)
        .put("libraries", libraries)
    listOf("mainClass", "arguments", "minecraftArguments", "type").forEach { key ->
        if (loaderProfile.has(key)) merged.put(key, loaderProfile.get(key))
    }
    if (loaderProfile.has("logging")) merged.put("logging", loaderProfile.get("logging"))
    return merged
}
