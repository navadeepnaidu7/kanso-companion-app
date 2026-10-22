package com.navadeep.kanso.companion.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteProfileTest {
    @Test fun unknownAppsAndHomeKeepNavigation() {
        assertEquals(RemoteProfile.Navigation, RemoteProfile.forApp(null))
        assertEquals(RemoteProfile.Navigation, RemoteProfile.forApp(TvApp("localsend", "LocalSend")))
        assertEquals(RemoteProfile.Navigation, RemoteProfile.forApp(TvApp("settings", "Settings")))
    }
    @Test fun declaredCategoriesTakePriorityOverNameFallback() {
        assertEquals(RemoteProfile.Audio, RemoteProfile.forApp(TvApp("youtube", "YouTube", category = 1)))
        assertEquals(RemoteProfile.Video, RemoteProfile.forApp(TvApp("unknown", "New player", category = 2)))
    }
    @Test fun youtubeMusicGetsTrackControlsInsteadOfSeeking() {
        assertEquals(RemoteProfile.Audio, RemoteProfile.forApp(TvApp("com.google.android.youtube.tvmusic", "YouTube Music")))
        assertEquals(RemoteProfile.Video, RemoteProfile.forApp(TvApp("com.google.android.youtube.tv", "YouTube")))
        assertEquals(RemoteProfile.Video, RemoteProfile.forApp(TvApp("nuvio", "Nuvio")))
    }
}
