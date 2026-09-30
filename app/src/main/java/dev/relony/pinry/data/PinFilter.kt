package dev.relony.pinry.data

import kotlinx.serialization.Serializable

/** Which pins a feed shows; maps onto Pinry's pin list filters. */
@Serializable
sealed interface PinFilter {
    @Serializable data object All : PinFilter
    @Serializable data class Tag(val name: String) : PinFilter
    @Serializable data class User(val username: String) : PinFilter
    @Serializable data class Board(val id: Int, val name: String) : PinFilter
}
