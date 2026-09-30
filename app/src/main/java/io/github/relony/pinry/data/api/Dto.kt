package io.github.relony.pinry.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val PinryJson = Json {
    ignoreUnknownKeys = true
    // Nulls are left out of request bodies, so a PATCH only touches the fields that were set.
    explicitNulls = false
}

@Serializable
data class User(
    val username: String,
    val email: String? = null,
    /** md5 of the email, for gravatar.com. */
    val gravatar: String? = null,
    /** Only present for the logged-in user. */
    val token: String? = null,
)

@Serializable
data class ImageSize(val image: String, val width: Int, val height: Int)

@Serializable
data class PinImage(
    val id: Int,
    val image: String,
    val width: Int,
    val height: Int,
    val standard: ImageSize? = null,
    val thumbnail: ImageSize? = null,
    val square: ImageSize? = null,
)

@Serializable
data class Pin(
    val id: Int,
    @SerialName("private") val isPrivate: Boolean = false,
    val submitter: User,
    /** Where the image was downloaded from, if it was pinned from a URL. */
    val url: String? = null,
    val description: String? = null,
    /** The page the image was found on. */
    val referer: String? = null,
    val image: PinImage,
    val tags: List<String> = emptyList(),
)

@Serializable
data class Board(
    val id: Int,
    val name: String,
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("total_pins") val totalPins: Int = 0,
    val cover: Pin? = null,
    val submitter: User,
)

@Serializable
data class BoardName(val id: Int, val name: String)

@Serializable
data class TagName(val name: String)

@Serializable
data class Page<T>(val count: Int, val next: String? = null, val results: List<T>)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class NewPin(
    val url: String? = null,
    val referer: String? = null,
    @SerialName("image_by_id") val imageId: Int? = null,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean = false,
    val tags: List<String> = emptyList(),
)

/** Pinry wipes a pin's tags when a PATCH leaves them out, so [tags] has no default and is always sent. */
@Serializable
data class PinUpdate(
    val tags: List<String>,
    val description: String? = null,
    @SerialName("private") val isPrivate: Boolean? = null,
)

@Serializable
data class NewBoard(val name: String, @SerialName("private") val isPrivate: Boolean = false)

@Serializable
data class BoardUpdate(
    val name: String? = null,
    @SerialName("private") val isPrivate: Boolean? = null,
    @SerialName("pins_to_add") val pinsToAdd: List<Int>? = null,
    @SerialName("pins_to_remove") val pinsToRemove: List<Int>? = null,
)
