package eu.kanade.tachiyomi.animeextension.en.animekhor

import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.ParsedAnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class AnimeKhor : ParsedAnimeHttpSource() {

    override val name = "AnimeKhor"
    override val baseUrl = "https://animekhor.org"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient = network.client

    // Popular / Latest
    override fun popularAnimeRequest(page: Int): Request = GET("$baseUrl/page/$page/")
    override fun popularAnimeSelector(): String = "article.fstrp"
    override fun popularAnimeFromElement(element: Element): SAnime {
        val anime = SAnime.create()
        anime.setUrlWithoutDomain(element.select("a").attr("href"))
        anime.title = element.select("h2.entry-title, .title").text()
        anime.thumbnail_url = element.select("img").attr("abs:src")
        return anime
    }
    override fun popularAnimeNextPageSelector(): String = "a.next.page-numbers"

    override fun latestUpdatesRequest(page: Int): Request = popularAnimeRequest(page)
    override fun latestUpdatesSelector(): String = popularAnimeSelector()
    override fun latestUpdatesFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun latestUpdatesNextPageSelector(): String = popularAnimeNextPageSelector()

    // Search
    override fun searchAnimeRequest(page: Int, query: String, filters: eu.kanade.tachiyomi.animesource.model.AnimeFilterList): Request =
        GET("$baseUrl/page/$page/?s=$query")
    override fun searchAnimeSelector(): String = popularAnimeSelector()
    override fun searchAnimeFromElement(element: Element): SAnime = popularAnimeFromElement(element)
    override fun searchAnimeNextPageSelector(): String = popularAnimeNextPageSelector()

    // Details
    override fun animeDetailsParse(document: Document): SAnime {
        val anime = SAnime.create()
        anime.title = document.select("h1.entry-title").text()
        anime.description = document.select("div.entry-content p").text()
        return anime
    }

    // Episode List
    override fun episodeListSelector(): String = "div.eplister ul li a"
    override fun episodeFromElement(element: Element): SEpisode {
        val episode = SEpisode.create()
        episode.setUrlWithoutDomain(element.attr("href"))
        episode.name = element.select(".epl-num").text() + " - " + element.select(".epl-title").text()
        return episode
    }

    // Extract direct streams (Skips all website popups & overlay ads)
    override fun videoListSelector(): String = "iframe, video"
    override fun videoListParse(document: Document): List<Video> {
        val videos = mutableListOf<Video>()
        
        document.select("iframe").forEach { iframe ->
            val embedUrl = iframe.attr("abs:src")
            if (embedUrl.isNotEmpty() && !embedUrl.contains("ads")) {
                videos.add(Video(embedUrl, "Stream Player", embedUrl))
            }
        }
        
        val html = document.html()
        Regex("""https?://[^\s"']+\.(?:m3u8|mp4)(?:\?[^\s"']*)?""").findAll(html).forEach { match ->
            videos.add(Video(match.value, "Direct Stream", match.value))
        }

        return videos
    }

    override fun videoFromElement(element: Element): Video = throw UnsupportedOperationException()
    override fun videoUrlParse(document: Document): String = throw UnsupportedOperationException()
}
