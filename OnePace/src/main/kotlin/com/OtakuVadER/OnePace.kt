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

    override val supportedTypes = setOf(TvType.Anime)

    override val mainPage = mainPageOf(
        "/series/one-pace-english-sub/" to "One Pace English Sub",
        "/series/one-pace-english-dub/" to "One Pace English Dub",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest,
    ): HomePageResponse {
        val link = "$mainUrl${request.data}"
        val document = app.get(link).document
        val home = document.select("div.seasons.aa-crd > div.seasons-bx").map {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): AnimeSearchResponse {
        val hreftitle = this.selectFirst("picture img")?.attr("alt") ?: ""
        val href = if (hreftitle.contains("Dub", ignoreCase = true)) {
            "https://onepace.me/series/one-pace-english-dub"
        } else {
            "https://onepace.me/series/one-pace-english-sub"
        }
        val title = this.selectFirst("p")?.text() ?: ""
        val posterUrl = this.selectFirst("img")?.attr("data-src")
        val isDub = hreftitle.contains("Dub", ignoreCase = true)

        return newAnimeSearchResponse(title, Media(href, posterUrl, title).toJson(), TvType.Anime, false) {
            this.posterUrl = posterUrl
            addDubStatus(dubExist = isDub, subExist = !isDub)
        }
    }

    override suspend fun search(query: String): List<AnimeSearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("ul[data-results] li article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val media = parseJson<Media>(url)
        val document = app.get(media.url).document
        val arcInt = media.mediaType?.substringAfter("Arc ")
        val element = document.selectFirst("div.seasons.aa-crd > div.seasons-bx:contains(S$arcInt-)")

        val title = media.mediaType ?: "No Title"
        val poster = "https://images3.alphacoders.com/134/1342304.jpeg"
        val plot = document.selectFirst("div.entry-content p")?.text()?.trim()
            ?: document.selectFirst("meta[name=twitter:description]")?.attr("content")
        val year = (document.selectFirst("span.year")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:updated_time]")?.attr("content")
                ?.substringBefore("-"))?.toIntOrNull()
        val lst = element?.select("ul.seasons-lst.anm-a li")

        // Debugging logs to inspect if the `element` and `lst` are correctly populated
        Log.d("OnepaceProvider", "Element found: $element")
        Log.d("OnepaceProvider", "List of episodes: $lst")

        return if (lst == null || lst.isEmpty()) {
            // If no episodes are found, log this and return as a Movie type (default behavior)
            Log.w("OnepaceProvider", "No episodes found for $title; returning as a Movie")
            newMovieLoadResponse(title, url, TvType.Movie, Media(
                media.url,
                mediaType = 1
            ).toJson()) {
                this.posterUrl = poster
                this.plot = plot
                this.year = year
            }
        } else {
            // If episodes are found, parse and return them as a TvSeries
            val episodes = lst.mapNotNull {
                val name = it.selectFirst("h3.title")?.ownText() ?: return@mapNotNull null
                val href = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val seasonNumberText = it.selectFirst("h3.title > span")?.text()
                val seasonNumber = seasonNumberText?.substringAfter("S")?.substringBefore("-")?.toIntOrNull()
                val episodePoster = "https://raw.githubusercontent.com/phisher98/TVVVV/refs/heads/main/OnePack.png"

                // Debug each episode being parsed
                Log.d("OnepaceProvider", "Parsed episode: $name, URL: $href, Season: $seasonNumber")
                
                Episode(Media(href, mediaType = 2).toJson(), name, posterUrl = episodePoster, season = seasonNumber)
            }

            // Return as a TvSeries if episodes are found
            Log.i("OnepaceProvider", "Returning $title as a TvSeries with ${episodes.size} episodes")
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
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
