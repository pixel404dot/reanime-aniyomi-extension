package eu.kanade.tachiyomi.animeextension.en.reanime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SearchResponse(
    val results: List<AnimeCard> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
)

@Serializable
class HomeResponse(
    @SerialName("latest_aired") val latestAired: List<AnimeCard> = emptyList(),
    @SerialName("new_on_site") val newOnSite: List<AnimeCard> = emptyList(),
    val trending: List<AnimeCard> = emptyList(),
)

@Serializable
class AnimeCard(
    @SerialName("anime_id") val animeId: String,
    val title: Title? = null,
    @SerialName("cover_image") val coverImage: CoverImage? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
)

@Serializable
class Title(
    val english: String? = null,
    val romaji: String? = null,
    val native: String? = null,
    @SerialName("user_preferred") val userPreferred: String? = null,
) {
    fun best(): String =
        listOfNotNull(english, userPreferred, romaji, native)
            .firstOrNull { it.isNotBlank() }
            ?: "Unknown"
}

@Serializable
class CoverImage(
    @SerialName("extra_large") val extraLarge: String? = null,
    val large: String? = null,
    val medium: String? = null,
) {
    fun best(): String? = extraLarge ?: large ?: medium
}

@Serializable
class AnimeDetails(
    @SerialName("anime_id") val animeId: String,
    val title: Title? = null,
    val description: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<Studio> = emptyList(),
    @SerialName("cover_image") val coverImage: CoverImage? = null,
    @SerialName("average_score") val averageScore: Int? = null,
    val rating: String? = null,
    val format: String? = null,
    val season: String? = null,
    @SerialName("season_year") val seasonYear: Int? = null,
)

@Serializable
class Studio(
    val name: String,
    @SerialName("is_main") val isMain: Boolean = false,
)

@Serializable
class EpisodeListResponse(
    val data: List<EpisodeDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val totalPages: Int = 0,
)

@Serializable
class EpisodeDto(
    @SerialName("episodeId") val episodeId: String? = null,
    @SerialName("episode_number") val episodeNumber: Float = 0f,
    val title: String? = null,
    val aired: String? = null,
    val playable: Boolean = true,
    val subbed: Boolean = false,
    val dubbed: Boolean = false,
    @SerialName("is_filler") val isFiller: Boolean = false,
)
