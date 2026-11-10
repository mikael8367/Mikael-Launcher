package com.mikael.launcher.ui.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikael.launcher.ui.theme.OutlineSoft
import com.mikael.launcher.ui.theme.PrimaryGreen
import com.mikael.launcher.ui.theme.Surface as PanelSurface
import com.mikael.launcher.ui.theme.TextPrimary
import com.mikael.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val MOD_PAGE_SIZE = 20
private const val MOD_PREFS = "mikael_launcher_mod_sources"
private const val CURSE_KEY = "curseforge_api_key"

private data class CatalogMod(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val downloads: Long,
    val source: String,
    val pageUrl: String
)

@Composable
fun ModCatalogScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var gameVersion by rememberSaveable { mutableStateOf("1.12.2") }
    var loader by rememberSaveable { mutableStateOf("Forge") }
    var source by rememberSaveable { mutableStateOf("Modrinth") }
    var query by rememberSaveable { mutableStateOf("") }
    var mods by remember { mutableStateOf<List<CatalogMod>>(emptyList()) }
    var page by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var reloadRequested by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("Carregando mods…") }
    var searchToken by remember { mutableStateOf(0) }
    val apiKey = remember { context.getSharedPreferences(MOD_PREFS, Context.MODE_PRIVATE).getString(CURSE_KEY, "").orEmpty() }

    fun load(reset: Boolean) {
        if (loading) { if (reset) reloadRequested = true; return }
        if (!reset && !hasMore) return
        loading = true
        val requestedPage = if (reset) 0 else page
        val currentVersion = gameVersion
        val currentLoader = loader
        val currentSource = source
        val currentQuery = query.trim()
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ModCatalogApi.search(
                        version = currentVersion,
                        loader = currentLoader,
                        source = currentSource,
                        query = currentQuery,
                        offset = requestedPage * MOD_PAGE_SIZE,
                        curseKey = apiKey
                    )
                }
                mods = if (reset) result.mods else (mods + result.mods).distinctBy { it.source + ":" + it.id }
                page = requestedPage + 1
                hasMore = result.mods.size >= MOD_PAGE_SIZE
                message = result.notice ?: if (mods.isEmpty()) "Nenhum mod encontrado para esses filtros." else ""
            } catch (error: Exception) {
                message = error.message ?: "Não foi possível consultar os catálogos."
                if (reset) mods = emptyList()
                hasMore = false
            } finally {
                loading = false
                if (reloadRequested) {
                    reloadRequested = false
                    load(true)
                }
            }
        }
    }

    LaunchedEffect(Unit) { load(true) }
    LaunchedEffect(searchToken) { if (searchToken > 0) load(true) }
    LaunchedEffect(listState, mods.size, loading, hasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { lastVisible ->
                if (!loading && hasMore && mods.isNotEmpty() && lastVisible >= mods.size - 2) load(false)
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("BIBLIOTECA DE MODS", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Mods e modpacks", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                Text("Escolha a versão do Minecraft antes de procurar mods compatíveis.", color = TextSecondary, fontSize = 12.sp)
                Text("VERSÃO DO MINECRAFT", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("1.12.2", "1.16.5", "1.18.2", "1.19.2", "1.20.1", "1.21.1").forEach {
                        FilterChip(selected = gameVersion == it, onClick = { if (gameVersion != it) { gameVersion = it; searchToken++ } }, label = { Text(it, fontSize = 10.sp) })
                    }
                }
                Text("MOD LOADER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Forge", "Fabric", "NeoForge", "Quilt", "Todos").forEach {
                        FilterChip(selected = loader == it, onClick = { if (loader != it) { loader = it; searchToken++ } }, label = { Text(it, fontSize = 10.sp) })
                    }
                }
                Text("FONTE DOS MODS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Modrinth", "CurseForge", "Ambos").forEach {
                        FilterChip(selected = source == it, onClick = { if (source != it) { source = it; searchToken++ } }, label = { Text(it, fontSize = 10.sp) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Pesquisar mods") }
                    )
                    Button(onClick = { searchToken++ }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Pesquisar")
                    }
                }
                Text("Mostrando resultados para Minecraft $gameVersion • $loader", color = TextSecondary, fontSize = 11.sp)
            }
        }
        if (mods.isEmpty() && loading) {
            item { CircularProgressIndicator(modifier = Modifier.padding(20.dp), color = PrimaryGreen) }
        }
        items(mods, key = { it.source + ":" + it.id }) { mod ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                color = PanelSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OutlineSoft)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = PrimaryGreen.copy(alpha = 0.13f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.Extension, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.padding(10.dp).size(23.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(mod.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(mod.description.ifBlank { "Sem descrição disponível." }, color = TextSecondary, fontSize = 10.sp, maxLines = 2)
                        Text("${mod.source} • por ${mod.author} • ${mod.downloads} downloads", color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
        if (message.isNotBlank()) {
            item { Text(message, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(14.dp)) }
        }
        if (loading && mods.isNotEmpty()) {
            item { CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(22.dp), color = PrimaryGreen) }
        } else if (!hasMore && mods.isNotEmpty()) {
            item { Text("Você chegou ao fim dos resultados.", color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun CurseForgeApiSettings() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(MOD_PREFS, Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(CURSE_KEY, "").orEmpty()) }
    var saved by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("CATÁLOGO DE MODS", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("CurseForge API", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("Cole sua chave de API do CurseForge. Ela será salva no armazenamento privado deste aplicativo e usada somente quando CurseForge estiver selecionado.", color = TextSecondary, fontSize = 11.sp)
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Sua API Key") },
            visualTransformation = PasswordVisualTransformation()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                prefs.edit().putString(CURSE_KEY, apiKey.trim()).apply()
                saved = true
            }) { Text("Salvar API") }
            androidx.compose.material3.OutlinedButton(onClick = {
                apiKey = ""
                prefs.edit().remove(CURSE_KEY).apply()
                saved = true
            }) { Text("Apagar chave") }
        }
        if (saved) Text("Configuração salva neste aparelho.", color = PrimaryGreen, fontSize = 10.sp)
        Text("Modrinth não precisa de chave de API. A chave do CurseForge é fornecida por você; não coloque uma chave pessoal em repositórios públicos.", color = TextSecondary, fontSize = 10.sp)
    }
}

private data class ModSearchResult(val mods: List<CatalogMod>, val notice: String? = null)

private object ModCatalogApi {
    fun search(version: String, loader: String, source: String, query: String, offset: Int, curseKey: String): ModSearchResult {
        val found = mutableListOf<CatalogMod>()
        val notices = mutableListOf<String>()
        if (source == "Modrinth" || source == "Ambos") {
            try { found += searchModrinth(version, loader, query, offset) }
            catch (e: Exception) { notices += "Modrinth: ${e.message ?: "falha na consulta"}" }
        }
        if (source == "CurseForge" || source == "Ambos") {
            if (curseKey.isBlank()) {
                notices += "Adicione sua API do CurseForge em Configurações para consultar essa fonte."
            } else {
                try { found += searchCurseForge(version, loader, query, offset, curseKey) }
                catch (e: Exception) { notices += "CurseForge: ${e.message ?: "falha na consulta"}" }
            }
        }
        if (found.isEmpty() && notices.isNotEmpty()) throw IllegalStateException(notices.joinToString(" "))
        return ModSearchResult(found, notices.joinToString(" ").ifBlank { null })
    }

    private fun searchModrinth(version: String, loader: String, query: String, offset: Int): List<CatalogMod> {
        val facets = JSONArray().put(JSONArray().put("project_type:mod")).put(JSONArray().put("versions:$version"))
        if (loader != "Todos") facets.put(JSONArray().put("categories:${loader.lowercase()}"))
        val url = "https://api.modrinth.com/v2/search?query=${enc(query)}&facets=${enc(facets.toString())}&limit=$MOD_PAGE_SIZE&offset=$offset&index=relevance"
        val json = JSONObject(request(url, null))
        val hits = json.optJSONArray("hits") ?: JSONArray()
        return (0 until hits.length()).mapNotNull { i ->
            val item = hits.optJSONObject(i) ?: return@mapNotNull null
            CatalogMod(
                id = item.optString("project_id"),
                name = item.optString("title", "Mod sem nome"),
                description = item.optString("description"),
                author = item.optString("author", "Desconhecido"),
                downloads = item.optLong("downloads"),
                source = "Modrinth",
                pageUrl = "https://modrinth.com/mod/${item.optString("slug")}"
            )
        }
    }

    private fun searchCurseForge(version: String, loader: String, query: String, offset: Int, key: String): List<CatalogMod> {
        val loaderId = when (loader) { "Forge" -> 1; "Fabric" -> 4; "NeoForge" -> 6; "Quilt" -> 5; else -> 0 }
        val params = "gameId=432&classId=6&gameVersion=${enc(version)}&modLoaderType=$loaderId&sortField=2&sortOrder=desc&pageSize=$MOD_PAGE_SIZE&index=$offset&searchFilter=${enc(query)}"
        val json = JSONObject(request("https://api.curseforge.com/v1/mods/search?$params", key))
        val data = json.optJSONArray("data") ?: JSONArray()
        return (0 until data.length()).mapNotNull { i ->
            val item = data.optJSONObject(i) ?: return@mapNotNull null
            val authors = item.optJSONArray("authors")
            val author = authors?.optJSONObject(0)?.optString("name").orEmpty().ifBlank { "Desconhecido" }
            val links = item.optJSONObject("links")
            CatalogMod(
                id = item.optString("id"),
                name = item.optString("name", "Mod sem nome"),
                description = item.optString("summary"),
                author = author,
                downloads = item.optLong("downloadCount"),
                source = "CurseForge",
                pageUrl = links?.optString("websiteUrl").orEmpty()
            )
        }
    }

    private fun request(address: String, apiKey: String?): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/json")
            if (apiKey == null) setRequestProperty("User-Agent", "MikaelLauncher/0.1.0 (https://github.com/mikael8367/Mikael-Launcher)")
            else setRequestProperty("x-api-key", apiKey)
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("HTTP $code: ${body.take(180)}")
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
}
