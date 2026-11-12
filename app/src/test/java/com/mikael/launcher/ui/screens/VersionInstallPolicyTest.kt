package com.mikael.launcher.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Política de instalação da tela de versões: loaders com backend FCL baixam o
 * JSON oficial da versão-base por conta própria e não podem ser bloqueados pela
 * ausência do catálogo da Mojang na UI. Vanilla/Fabric/Quilt precisam da entrada
 * oficial (URL e SHA-1 do manifesto) para instalar a base antes do perfil.
 */
class VersionInstallPolicyTest {
    @Test
    fun vanillaRequiresTheOfficialManifestEntry() {
        assertTrue(installRequiresOfficialManifest("Vanilla"))
    }

    @Test
    fun fabricAndQuiltRequireTheOfficialManifestEntry() {
        // O instalador de perfil deles consome meta.fabricmc.net/meta.quiltmc.org,
        // mas a base vanilla é instalada primeiro via MinecraftInstaller, que
        // precisa da URL/SHA-1 vindos do catálogo oficial.
        assertTrue(installRequiresOfficialManifest("Fabric"))
        assertTrue(installRequiresOfficialManifest("Quilt"))
    }

    @Test
    fun fclBackedLoadersInstallWithoutTheCatalogBeingLoaded() {
        for (loader in listOf("Forge", "NeoForge", "OptiFine", "Legacy Fabric", "LiteLoader", "Forge + OptiFine")) {
            assertFalse("O loader $loader deve instalar sem o catálogo da Mojang carregado", installRequiresOfficialManifest(loader))
        }
    }
}
