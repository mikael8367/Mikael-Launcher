package com.mikael.launcher.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModCatalogPagingTest {
    @Test
    fun stopsWhenPageIsNotFull() {
        assertFalse(shouldContinuePaging(previousTotal = 20, updatedTotal = 35, pageRawSize = 15, pageSize = 20))
    }

    @Test
    fun stopsWhenMergeAddsNoNewItems() {
        // Fonte "Ambos": página cheia, mas todos os itens já existiam (duplicados).
        assertFalse(shouldContinuePaging(previousTotal = 40, updatedTotal = 40, pageRawSize = 20, pageSize = 20))
    }

    @Test
    fun continuesWhenPageIsFullAndNewItemsArrived() {
        assertTrue(shouldContinuePaging(previousTotal = 20, updatedTotal = 40, pageRawSize = 20, pageSize = 20))
    }

    @Test
    fun stopsOnShortFirstPage() {
        assertFalse(shouldContinuePaging(previousTotal = 0, updatedTotal = 3, pageRawSize = 3, pageSize = 20))
    }
}
