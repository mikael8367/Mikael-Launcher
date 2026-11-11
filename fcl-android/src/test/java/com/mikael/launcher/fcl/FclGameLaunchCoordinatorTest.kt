package com.mikael.launcher.fcl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FclGameLaunchCoordinatorTest {
    @Test
    fun acceptsValidOfflinePlayerNames() {
        assertTrue(FclGameLaunchCoordinator.isValidOfflineUsername("A"))
        assertTrue(FclGameLaunchCoordinator.isValidOfflineUsername("Player"))
        assertTrue(FclGameLaunchCoordinator.isValidOfflineUsername("Mikael_123"))
        assertTrue(FclGameLaunchCoordinator.isValidOfflineUsername("A".repeat(16)))
    }

    @Test
    fun rejectsInvalidOfflinePlayerNames() {
        assertFalse(FclGameLaunchCoordinator.isValidOfflineUsername(""))
        assertFalse(FclGameLaunchCoordinator.isValidOfflineUsername("player name"))
        assertFalse(FclGameLaunchCoordinator.isValidOfflineUsername("player-name"))
        assertFalse(FclGameLaunchCoordinator.isValidOfflineUsername("A".repeat(17)))
        assertFalse(FclGameLaunchCoordinator.isValidOfflineUsername("Jogadorç"))
    }
}
