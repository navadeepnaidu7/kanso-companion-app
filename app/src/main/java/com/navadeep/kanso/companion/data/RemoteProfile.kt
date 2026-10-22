package com.navadeep.kanso.companion.data

enum class RemoteProfile { Navigation, Video, Audio;
    companion object {
        fun forApp(app: TvApp?): RemoteProfile {
            if (app == null) return Navigation
            // Android's declared app category takes priority; older TV apps often omit it.
            if (app.category == 1) return Audio
            if (app.category == 2) return Video
            val identity = "${app.id} ${app.name}".lowercase()
            if (listOf("spotify", "youtube music", "youtubemusic", "tidal", "deezer", "music").any(identity::contains)) return Audio
            if (listOf("youtube", "netflix", "nuvio", "prime video", "amazonvideo", "hotstar", "stremio",
                    "disney", "plex", "crunchyroll", "apple tv", "appletv", "vlc", "jiotv", "sonyliv", "zee5", "kodi").any(identity::contains)) return Video
            return Navigation
        }
    }
}
