package com.navadeep.kanso.companion.data

import org.junit.Assert.*
import org.junit.Test

class AppArrangementTest {
    private val apps = listOf(TvApp("help", "Help"), TvApp("browser", "BrowseHere"),
        TvApp("yt", "YouTube"), TvApp("music", "YouTube Music"), TvApp("n", "Netflix"),
        TvApp("prime", "Prime Video"), TvApp("nuvio", "Nuvio"))

    @Test fun streamingAppsLeadUtilitiesAndMusicDoesNotDuplicateTheDefaultDock() {
        assertEquals(listOf("yt", "n", "nuvio", "prime"), AppArrangement.dock(apps, null))
        assertEquals(listOf("browser", "help"), AppArrangement.library(apps).takeLast(2).map { it.id })
    }
    @Test fun customOrderSurvivesPackageListChangesWithoutDuplicatesOrStaleApps() {
        assertEquals(listOf("prime", "yt"), AppArrangement.dock(apps, listOf("prime", "missing", "yt", "prime")))
    }
    @Test fun deliberatelyEmptyDockStaysEmpty() {
        assertTrue(AppArrangement.dock(apps, emptyList()).isEmpty())
    }
    @Test fun reorderPreservesTheOtherSlotsAndIgnoresInvalidMoves() {
        val dock = listOf("yt", "n", "nuvio", "prime")
        assertEquals(listOf("n", "yt", "nuvio", "prime"), AppArrangement.move(dock, "yt", 1))
        assertEquals(dock, AppArrangement.move(dock, "yt", -1))
        assertEquals(dock, AppArrangement.move(dock, "missing", 1))
    }
}
