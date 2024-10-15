package com.OtakuVadER

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson

open class OnepaceProvider : MainAPI() {
    override var mainUrl = "https://onepace.me"
    override var name = "OnePace"
    override val hasMainPage = true
    override var lang = "en"

    override val supportedTypes = setOf(
        TvType.Anime
    )

    // List all arcs separately on the homepage
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val arcs = ArcPosters.arcPosters
        val home = arcs.map { (arcNumber, arcPosterUrl) ->
            val title = "One Pace Arc $arcNumber"
            val arcUrl = "$mainUrl/arc-$arcNumber/"
            newAnimeSearchResponse(title, Media(arcUrl, arcPosterUrl, title).toJson(), TvType.Anime, false) {
                this.posterUrl = arcPosterUrl
            }
        }
        return newHomePageResponse("One Pace Arcs", home)
    }

    // This function searches for animes on the OnePace site
    override suspend fun search(query: String): List<AnimeSearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("ul[data-results] li article").mapNotNull {
            it.toSearchResult()
        }
    }

    // Load episodes for each arc individually
    override suspend fun load(url: String): LoadResponse {
        val media = parseJson<Media>(url)
        val document = app.get(media.url).document

        val title = media.mediaType ?: "No Title"
        val defaultPoster = "https://images3.alphacoders.com/134/1342304.jpeg"
        val plot = document.selectFirst("div.entry-content p")?.text()?.trim()
            ?: document.selectFirst("meta[name=twitter:description]")?.attr("content")
        val year = (document.selectFirst("span.year")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:updated_time]")?.attr("content")
                ?.substringBefore("-"))?.toIntOrNull()

        val episodeElements = document.select("ul.seasons-lst.anm-a li")

        return if (episodeElements.isEmpty()) {
            // Handle movies
            newMovieLoadResponse(title, url, TvType.Movie, Media(media.url, mediaType = 1).toJson()) {
                this.posterUrl = defaultPoster
                this.plot = plot
                this.year = year
            }
        } else {
            // Handle TV series (arcs with episodes)
            val episodes = episodeElements.mapNotNull {
                val episodeName = it.selectFirst("h3.title")?.ownText() ?: return@mapNotNull null
                val episodeUrl = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val seasonNumber = it.selectFirst("h3.title > span")?.text().toString().substringAfter("S").substringBefore("-")
                val season = seasonNumber.toIntOrNull()

                // Assign poster from ArcPosters
                val arcPoster = ArcPosters.arcPosters[season] ?: defaultPoster
                Episode(Media(episodeUrl, mediaType = 2).toJson(), episodeName, posterUrl = arcPoster, season = season)
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = defaultPoster
                this.plot = plot
                this.year = year
            }
        }
    }

    // Load media links (e.g., video links)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val media = parseJson<Media>(data)
        val bodyClass = app.get(media.url).document.selectFirst("body")?.attr("class") ?: return false
        val termId = Regex("""(?:term|postid)-(\d+)""").find(bodyClass)?.groupValues?.get(1)
            ?: throw ErrorLoadingException("No ID found")

        for (i in 0..4) {
            val iframeSrc = app.get("$mainUrl/?trdekho=$i&trid=$termId&trtype=${media.mediaType}")
                .document.selectFirst("iframe")?.attr("src")
                ?: throw ErrorLoadingException("No iframe found")

            loadExtractor(iframeSrc, subtitleCallback, callback)
        }
        return true
    }

    data class Media(val url: String, val poster: String? = null, val mediaType: String? = null)
}
