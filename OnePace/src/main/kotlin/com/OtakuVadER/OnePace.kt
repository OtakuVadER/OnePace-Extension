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

    // Show separate entries for each arc on the homepage
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        // Define each arc entry with its title, URL, and poster
        val arcs = listOf(
            ArcEntry("Romance Dawn Arc (Sub)", "$mainUrl/arc-1-sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-romance-dawn-arc.jpeg.webp"),
            ArcEntry("Orange Town Arc (Sub)", "$mainUrl/arc-2-sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-orange-town-arc.jpeg.webp"),
            ArcEntry("Syrup Village Arc (Sub)", "$mainUrl/arc-3-sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-syrup-village-arc.jpeg.webp"),
            ArcEntry("Gaimon Arc (Sub)", "$mainUrl/arc-4-sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-gaimon-arc.jpeg.webp"),
            ArcEntry("Baratie Arc (Sub)", "$mainUrl/arc-5-sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-baratie-arc.jpeg.webp"),
            ArcEntry("Arlong Park Arc (Sub)", "$mainUrl/arc-6-sub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-arlong-park-arc.jpeg.webp"),
            // Add all remaining arcs here, both Dub and Sub...
            ArcEntry("Romance Dawn Arc (Dub)", "$mainUrl/arc-1-dub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-romance-dawn-arc.jpeg.webp"),
            ArcEntry("Orange Town Arc (Dub)", "$mainUrl/arc-2-dub", "https://onepace.me/wp-content/webp-express/webp-images/uploads/2024/09/cover-orange-town-arc.jpeg.webp"),
            // Continue for the rest of the arcs...
        )

        // Create homepage entries for each arc
        val homeEntries = arcs.map { arc ->
            newAnimeSearchResponse(arc.title, Media(arc.url, arc.posterUrl).toJson(), TvType.Anime, false) {
                this.posterUrl = arc.posterUrl
            }
        }

        return newHomePageResponse("One Pace Arcs", homeEntries)
    }

    // Load the respective arc's episodes when clicked
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: "No Title"
        val poster = document.selectFirst("div.post-thumbnail figure img")?.attr("src")
            ?: "https://images3.alphacoders.com/134/1342304.jpeg"  // Default poster if not found
        val plot = document.selectFirst("div.entry-content p")?.text()?.trim()
            ?: document.selectFirst("meta[name=twitter:description]")?.attr("content")

        // Check if there are episodes for the specific arc
        val episodeElements = document.select("ul.seasons-lst.anm-a li")

        return if (episodeElements.isEmpty()) {
            // Handle as a movie or standalone content if no episodes found
            newMovieLoadResponse(title, url, TvType.Movie) {
                this.posterUrl = poster
                this.plot = plot
            }
        } else {
            // Load episodes for the arc
            val episodes = episodeElements.mapNotNull {
                val episodeTitle = it.selectFirst("h3.title")?.ownText() ?: return@mapNotNull null
                val episodeUrl = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                Episode(Media(episodeUrl).toJson(), episodeTitle)
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
            }
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

    // ArcEntry data class for managing arc information
    data class ArcEntry(val title: String, val url: String, val posterUrl: String)

    data class Media(val url: String, val poster: String? = null, val mediaType: String? = null)
}
