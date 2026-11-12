package com.mikael.launcher.fcl

import com.tungsten.fclcore.game.GameComponentType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Garante que cada mod loader oferecido na tela de versões tem um patch id e
 * um componente existentes no FCL pinnado: o MojangDownloadProvider registra
 * uma VersionList para cada GameComponentType abaixo, e o DefaultGameBuilder
 * instala qualquer ComponentRemoteVersion pelo mesmo caminho genérico usado
 * por Forge/OptiFine. Ids desconhecidos quebrariam o catálogo e a instalação.
 */
class FclModLoaderCatalogTest {

    @Test
    fun `patch ids match FCL component registry`() {
        assertEquals("forge", FclModLoader.FORGE.patchId)
        assertEquals("neoforge", FclModLoader.NEOFORGE.patchId)
        assertEquals("optifine", FclModLoader.OPTIFINE.patchId)
        assertEquals("legacyfabric", FclModLoader.LEGACY_FABRIC.patchId)
        assertEquals("liteloader", FclModLoader.LITELOADER.patchId)
    }

    @Test
    fun `component types match FCL game components`() {
        assertEquals(GameComponentType.FORGE, FclModLoader.FORGE.componentType)
        assertEquals(GameComponentType.NEO_FORGE, FclModLoader.NEOFORGE.componentType)
        assertEquals(GameComponentType.OPTIFINE, FclModLoader.OPTIFINE.componentType)
        assertEquals(GameComponentType.LEGACY_FABRIC, FclModLoader.LEGACY_FABRIC.componentType)
        assertEquals(GameComponentType.LITELOADER, FclModLoader.LITELOADER.componentType)
    }

    @Test
    fun `every single loader patch id resolves in FCL`() {
        // FORGE_OPTIFINE é uma combinação instalada em duas etapas, não um
        // componente próprio; todos os demais precisam resolver no FCL.
        for (loader in FclModLoader.values()) {
            if (loader == FclModLoader.FORGE_OPTIFINE) continue
            assertEquals(loader.componentType, GameComponentType.fromPatchId(loader.patchId))
        }
    }
}
