package eu.kanade.tachiyomi.animeextension.en.reanime

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import keiyoushi.utils.AnimeHttpLegacySource
import keiyoushi.utils.parseAs
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

class Reanime : AnimeHttpLegacySource() {

    override val name = "Re:ANIME"
    override val baseUrl = "https://reanime.to"
    override val lang = "en"
    override val supportsLatest = true

    private val apiUrl = "$baseUrl/api/v1"
    private val pageSize = 24

    override fun headersBuilder() = super.headersBuilder()
        .add("Accept", "application/json")
        .add("Origin", baseUrl)
        .add("Referer", "$baseUrl/")

    override fun popularAnimeRequest(page: Int): Request {
        val offset = (page - 1) * pageSize
        val url = "$apiUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", "*")
            .addQueryParameter("sort", "popular")
            .addQueryParameter("limit", pageSize.toString())
            .addQueryParameter("offset", offset.toString())
            .build()
        return GET(url, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val data = response.parseAs<SearchResponse>()
        val animes = data.results.map { it.toSAnime() }
        val hasNext = data.offset + data.results.size < data.total
        return AnimesPage(animes, hasNext)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiUrl/home".toHttpUrl().newBuilder()
            .addQueryParameter("limit", pageSize.toString())
            .build()
        return GET(url, headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        val data = response.parseAs<HomeResponse>()
        return AnimesPage(data.latestAired.map { it.toSAnime() }, false)
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val offset = (page - 1) * pageSize
        val url = "$apiUrl/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", query.ifBlank { "*" })
            .addQueryParameter("limit", pageSize.toString())
            .addQueryParameter("offset", offset.toString())
            .build()
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = popularAnimeParse(response)

    override fun animeDetailsRequest(anime: SAnime): Request =
        GET("$apiUrl/anime/${anime.url.trimStart('/')}", headers)

    override fun getAnimeUrl(anime: SAnime): String =
        "$baseUrl/anime/${anime.url.trimStart('/')}"

    override fun animeDetailsParse(response: Response): SAnime {
        val details = response.parseAs<AnimeDetails>()
        return SAnime.create().apply {
            title = details.title?.best() ?: details.animeId
            url = details.animeId
            thumbnail_url = details.coverImage?.best()
            author = details.studios.firstOrNull { it.isMain }?.name
                ?: details.studios.firstOrNull()?.name
            artist = details.format
            genre = buildList {
                addAll(details.genres)
                details.season?.let { season ->
                    val year = details.seasonYear?.toString().orEmpty()
                    add(
                        listOf(
                            season.lowercase().replaceFirstChar { it.titlecase(Locale.US) },
                            year,
                        ).filter { it.isNotBlank() }.joinToString(" "),
                    )
                }
                details.rating?.let(::add)
            }.filter { it.isNotBlank() }.joinToString(", ")
            status = when (details.status?.lowercase(Locale.US)) {
                "releasing", "airing" -> SAnime.ONGOING
                "finished", "completed" -> SAnime.COMPLETED
                "cancelled", "canceled" -> SAnime.CANCELLED
                "hiatus" -> SAnime.ON_HIATUS
                else -> SAnime.UNKNOWN
            }
            description = buildString {
                details.description
                    ?.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                    ?.replace(Regex("<[^>]+>"), "")
                    ?.trim()
                    ?.let {
                        appendLine(it)
                        appendLine()
                    }
                details.averageScore?.let { appendLine("Score: $it") }
                details.format?.let { appendLine("Format: $it") }
            }.trim()
        }
    }

    override fun episodeListRequest(anime: SAnime): Request {
        val slug = anime.url.trimStart('/')
        val url = "$apiUrl/anime/$slug/episodes".toHttpUrl().newBuilder()
            .addQueryParameter("limit", "100")
            .addQueryParameter("offset", "0")
            .build()
        return GET(url, headers)
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val first = response.parseAs<EpisodeListResponse>()
        val slug = response.request.url.pathSegments.getOrNull(3)
            ?: error("Missing anime slug in episode request")

        val all = first.data.toMutableList()
        var offset = first.offset + first.data.size
        while (offset < first.total) {
            val url = "$apiUrl/anime/$slug/episodes".toHttpUrl().newBuilder()
                .addQueryParameter("limit", "100")
                .addQueryParameter("offset", offset.toString())
                .build()
            val page = client.newCall(GET(url, headers)).execute()
                .parseAs<EpisodeListResponse>()
            if (page.data.isEmpty()) break
            all += page.data
            offset += page.data.size
        }

        return all.filter { it.playable }.map { episode ->
            SEpisode.create().apply {
                val number = episode.episodeNumber.let { value ->
                    if (value % 1f == 0f) value.toInt().toString() else value.toString()
                }
                url = "$slug/$number"
                episode_number = episode.episodeNumber
                name = buildString {
                    append("Episode $number")
                    episode.title
                        ?.takeIf { it.isNotBlank() && !it.equals("Episode $number", true) }
                        ?.let { append(" - "); append(it.trim()) }
                    val flags = buildList {
                        if (episode.subbed) add("SUB")
                        if (episode.dubbed) add("DUB")
                        if (episode.isFiller) add("FILLER")
                    }
                    if (flags.isNotEmpty()) append(" [${flags.joinToString("/")}]")
                }
                date_upload = parseDate(episode.aired)
            }
        }.sortedByDescending { it.episode_number }
    }

    override fun videoListRequest(episode: SEpisode): Request {
        return GET("$baseUrl/anime/${episode.url.substringBefore("/")}", headers)
    }

    override fun videoListParse(response: Response): List<Video> {
        throw Exception(
            "Re:ANIME watch/server APIs are not public. " +
                "Catalog, search, details and episodes work; playback is not wired yet.",
        )
    }

    private fun AnimeCard.toSAnime() = SAnime.create().apply {
        title = this@toSAnime.title?.best() ?: animeId
        url = animeId
        thumbnail_url = coverImage?.best()
    }

    private fun parseDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
        )
        for (pattern in formats) {
            runCatching {
                val parsed = SimpleDateFormat(pattern, Locale.US).parse(raw)?.time
                if (parsed != null) return parsed
            }
        }
        return 0L
    }
}
