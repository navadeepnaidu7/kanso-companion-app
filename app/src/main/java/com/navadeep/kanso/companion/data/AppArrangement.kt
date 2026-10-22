package com.navadeep.kanso.companion.data

/** A stable entertainment-first library; user choices only change the dock. */
object AppArrangement {
    const val DockSize = 4
    private val preferred = listOf("youtube", "netflix", "nuvio", "prime video", "jiohotstar", "stremio",
        "disney", "spotify", "plex", "crunchyroll", "apple tv", "vlc", "jiotv", "sony", "zee")

    private fun rank(app: TvApp): Int {
        val name = app.name.lowercase()
        if (name.startsWith("youtube music")) return 8
        return preferred.indexOfFirst { name.startsWith(it) }.let { if (it < 0) Int.MAX_VALUE else it }
    }

    fun library(apps: List<TvApp>): List<TvApp> = apps.distinctBy { it.id }
        .sortedWith(compareBy<TvApp> { rank(it) }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    fun dock(apps: List<TvApp>, saved: List<String>?): List<String> {
        val available = apps.map { it.id }.toSet()
        return (saved ?: library(apps).map { it.id }).filter { it in available }.distinct().take(DockSize)
    }

    fun move(ids: List<String>, id: String, direction: Int): List<String> {
        val from = ids.indexOf(id)
        val to = from + direction
        if (from < 0 || to !in ids.indices) return ids
        return ids.toMutableList().apply { this[from] = ids[to]; this[to] = id }
    }
}
