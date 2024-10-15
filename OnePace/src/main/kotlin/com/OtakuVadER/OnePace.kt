package com.OtakuVadER

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import org.jsoup.nodes.Element

open class OnepaceProvider : MainAPI() {
    override var mainUrl = "https://onepace.me"
    override var name = "OnePace"
    override val hasMainPage = true
    override var lang = "en"

    override val supportedTypes = setOf(
        TvType.Anime
    )

    // Show two entries on the homepage: One Pace Dub and One Pace Sub
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        // Define Dub and Sub entries
        val homeEntries = listOf(
            newAnimeSearchResponse("One Pace Dub", Media("$mainUrl/dub", "").toJson(), TvType.Anime, false) {
                this.posterUrl = "https://onepace.me/wp-content/uploads/2021/05/dub-cover.jpg" // Example dub poster
            },
            newAnimeSearchResponse("One Pace Sub", Media("$mainUrl/sub", "").toJson(), TvType.Anime, false) {
                this.posterUrl = "https://onepace.me/wp-content/uploads/2021/05/sub-cover.jpg" // Example sub poster
            }
        )
        return newHomePageResponse("One Pace", homeEntries)
    }

    // This function loads the respective arcs as seasons for Dub or Sub
    override suspend fun load(url: String): LoadResponse {
        // Check if the URL is for Dub or Sub and load the respective arcs as seasons
        return when {
            url.contains("dub") -> loadDubArcs(url)
            url.contains("sub") -> loadSubArcs(url)
            else -> throw ErrorLoadingException("Unknown URL")
        }
    }

    // Load all dubbed arcs as seasons
    private suspend fun loadDubArcs(url: String): LoadResponse {
        val title = "One Pace Dub"
        val poster = "https://onepace.me/wp-content/uploads/2021/05/dub-cover.jpg" // Example dub poster

        // List of dubbed arcs (seasons)
        val dubbedArcs = listOf(
            ArcEntry("Romance Dawn Arc (Dub)", "$mainUrl/arc-1/dub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-romance-dawn-arc.jpeg.webp"),
            ArcEntry("Orange Town Arc (Dub)", "$mainUrl/arc-2/dub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-orange-town-arc.jpeg.webp")
            // Add all other dubbed arcs here...
        )

        // Create episodes for each dubbed arc (as seasons)
        val seasons = dubbedArcs.map { arc ->
            Episode(arc.url, arc.title, posterUrl = arc.posterUrl)
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, seasons) {
            this.posterUrl = poster
        }
    }

    // Load all subbed arcs as seasons
    private suspend fun loadSubArcs(url: String): LoadResponse {
        val title = "One Pace Sub"
        val poster = "https://onepace.me/wp-content/uploads/2021/05/sub-cover.jpg" // Example sub poster

        // List of subbed arcs (seasons)
        val subbedArcs = listOf(
            ArcEntry("Romance Dawn Arc (Sub)", "$mainUrl/arc-1/sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-romance-dawn-arc.jpeg.webp"),
            ArcEntry("Orange Town Arc (Sub)", "$mainUrl/arc-2/sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-orange-town-arc.jpeg.webp")
            // Add all other subbed arcs here...
        )

        // Create episodes for each subbed arc (as seasons)
        val seasons = subbedArcs.map { arc ->
            Episode(arc.url, arc.title, posterUrl = arc.posterUrl)
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, seasons) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val media = parseJson<Media>(data)
        val document = app.get(media.url).document
        val iframe = document.selectFirst("iframe")?.attr("src")
            ?: throw ErrorLoadingException("No iframe found")
        loadExtractor(iframe, subtitleCallback, callback)
        return true
    }

    // Data class to manage Arc entries
    data class ArcEntry(val title: String, val url: String, val posterUrl: String)
    
    data class Media(val url: String, val poster: String? = null, val mediaType: String? = null)
}
