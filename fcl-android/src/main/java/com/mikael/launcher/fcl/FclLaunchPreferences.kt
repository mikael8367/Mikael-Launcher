package com.mikael.launcher.fcl

import com.mio.data.Renderer
import com.tungsten.fcl.setting.VersionSetting

/**
 * Preferências escolhidas na tela de Configurações do Mikael Launcher que
 * precisam valer na inicialização do Minecraft Java.
 *
 * O FCL upstream lê renderer, memória e argumentos JVM do VersionSetting do
 * perfil (FCLGameRepository.getLaunchOptions usa vs.getRenderer(),
 * vs.getMaxMemory(), vs.isAutoMemory() e vs.getJavaArgs()), e o coordinator
 * escolhe o runtime Java a partir de setting.java. Esta classe carrega as
 * escolhas do Mikael até esse ponto; campos nulos deixam o valor atual do FCL
 * intocado.
 */
data class FclLaunchPreferences(
    /** Nome da pasta do runtime (ex.: "jre17"), ou null para deixar "Auto". */
    val runtimeFolder: String? = null,
    /** Rótulo do renderizador da UI do Mikael (ex.: "GL4ES"), ou null para não alterar. */
    val rendererLabel: String? = null,
    /** Limite de RAM em MB quando autoMemory = false, ou null. */
    val memoryMB: Int? = null,
    /** true = gerenciamento automático de RAM (padrão upstream do FCL). */
    val autoMemory: Boolean = true,
    /** Argumentos JVM digitados pelo usuário, ou null para não alterar. */
    val javaArgs: String? = null
)

/**
 * Mapeia o rótulo do renderizador da UI para o ID real usado pelo FCL
 * (com.mio.data.Renderer). Renderizadores sem equivalente no FCL upstream
 * (ANGLE, Vulkan experimental, Software) retornam null: o valor atual do
 * perfil é mantido em vez de gravar um ID inexistente, o que quebraria a
 * inicialização com "renderizador não encontrado".
 */
fun rendererIdForLabel(label: String?): String? = when (label?.trim()?.lowercase()) {
    "gl4es" -> Renderer.ID_GL4ES
    "zink" -> Renderer.ID_ZINK
    "vgpu" -> Renderer.ID_VGPU
    else -> null
}

/** "Java 17" -> "jre17". Rótulos inválidos retornam null (mantém "Auto"). */
fun runtimeFolderForLabel(label: String?): String? {
    val major = label?.trim()
        ?.takeIf { it.startsWith("Java ") }
        ?.removePrefix("Java ")
        ?.trim()
        ?.toIntOrNull()
        ?: return null
    return when (major) {
        8 -> "jre8"
        17 -> "jre17"
        21 -> "jre21"
        25 -> "jre25"
        else -> null
    }
}

/** "2048 MB" -> 2048. Aceita 256..8192 MB; inválido retorna null. */
fun parseMemoryMB(label: String?): Int? {
    val value = label?.trim()
        ?.removeSuffix("MB")
        ?.removeSuffix("mb")
        ?.trim()
        ?.toIntOrNull()
        ?: return null
    return if (value in 256..8192) value else null
}

/**
 * Remove -Xms/-Xmx dos argumentos JVM. A memória é controlada por
 * maxMemory/isAutoMemory; deixar -Xmx nos args faria o FCL anular o limite
 * automático (upstream zera maxMemory quando javaArgs contém -Xmx em modo
 * automático), misturando duas fontes de configuração.
 */
fun stripMemoryJvmArgs(args: String): String =
    args.split(Regex("\\s+"))
        .filter {
            it.isNotBlank() &&
                !it.startsWith("-Xms", ignoreCase = true) &&
                !it.startsWith("-Xmx", ignoreCase = true)
        }
        .joinToString(" ")

/**
 * Aplica as preferências do Mikael ao VersionSetting que o FCL usa para montar
 * os LaunchOptions. Função pura sobre o setting; campos nulos não alteram nada.
 */
fun applyFclLaunchPreferences(setting: VersionSetting, prefs: FclLaunchPreferences) {
    prefs.runtimeFolder?.let { setting.java = it }
    rendererIdForLabel(prefs.rendererLabel)?.let { setting.renderer = it }
    if (prefs.autoMemory) {
        setting.isAutoMemory = true
    } else {
        val memory = prefs.memoryMB
        if (memory != null) {
            setting.isAutoMemory = false
            setting.maxMemory = memory
        }
    }
    prefs.javaArgs?.let { setting.javaArgs = stripMemoryJvmArgs(it) }
}
