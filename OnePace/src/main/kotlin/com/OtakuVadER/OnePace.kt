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

    override val supportedTypes =
        setOf(
            TvType.Anime,
        )

    override val mainPage =
        mainPageOf(
            "/series/one-pace-english-sub/" to "One Pace English Sub",
            "/series/one-pace-english-dub/" to "One Pace English Dub"
        )

    // This function will fetch each arc individually and list them separately on the homepage
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest,
    ): HomePageResponse {
        val arcs = ArcPosters.arcPosters
        val home = arcs.map { (arcNumber, arcPosterUrl) ->
            // Create a separate entry for each arc
            val title = "Arc $arcNumber"
            val arcUrl = "$mainUrl/series/one-pace-arc-$arcNumber/"  // Assuming each arc has a unique URL pattern
            newAnimeSearchResponse(title, Media(arcUrl, arcPosterUrl, title).toJson(), TvType.Anime, false) {
                this.posterUrl = arcPosterUrl
            }
        }

        return newHomePageResponse("One Pace Arcs", home)
    }

    override suspend fun search(query: String): List<AnimeSearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("ul[data-results] li article").mapNotNull {
            it.toSearchResult()
        }
    }

    // This function will load episodes for each arc individually
    override suspend fun load(url: String): LoadResponse {

        val media = parseJson<Media>(url)
        val document = app.get(media.url).document

        // Use the mediaType or any other indicator to determine if this is a TV show or a movie
        val title = media.mediaType ?: "No Title"
        val defaultPoster = "https://images3.alphacoders.com/134/1342304.jpeg"  // Default poster in case no poster is found
        val plot = document.selectFirst("div.entry-content p")?.text()?.trim()
            ?: document.selectFirst("meta[name=twitter:description]")?.attr("content")
        val year = (document.selectFirst("span.year")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:updated_time]")?.attr("content")
                ?.substringBefore("-"))?.toIntOrNull()

        // Check if there are episodes for the specific arc
        val episodeElements = document.select("ul.seasons-lst.anm-a li")

        return if (episodeElements.isEmpty()) {
            // It's a movie
            newMovieLoadResponse(title, url, TvType.Movie, Media(
                media.url,
                mediaType = 1 // Assuming 1 stands for movies
            ).toJson()) {
                this.posterUrl = defaultPoster
                this.plot = plot
                this.year = year
            }
        } else {
            // Load episodes for the arc
            val episodes = episodeElements.mapNotNull {
                val name = it.selectFirst("h3.title")?.ownText() ?: "null"
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val seasonNumber = it.selectFirst("h3.title > span")?.text().toString().substringAfter("S").substringBefore("-")
                val season = seasonNumber.toIntOrNull()

                // Get the poster for the arc from ArcPosters.kt
                val arcPoster = ArcPosters.arcPosters[season] ?: defaultPoster

                Episode(Media(href, mediaType = 2).toJson(), name, posterUrl = arcPoster, season = season)
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = defaultPoster  // You can use a more specific poster for the series itself if available
                this.plot = plot
                this.year = year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean {
        val media = parseJson<Media>(data)
        val body = app.get(media.url).document.selectFirst("body")?.attr("class") ?: return false
        val term = Regex("""(?:term|postid)-(\d+)""").find(body)?.groupValues?.get(1) ?: throw ErrorLoadingException("no id found")
        for (i in 0..4) {
            val link = app.get("$mainUrl/?trdekho=$i&trid=$term&trtype=${media.mediaType}")
                .document.selectFirst("iframe")?.attr("src")
                ?: throw ErrorLoadingException("no iframe found")
            Log.d("VadER", link)
            loadExtractor(link, subtitleCallback, callback)
        }
        return true
    }

    data class Media(val url: String, val poster: String? = null, val mediaType: String? = null)
}
