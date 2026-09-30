@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.relony.pinry.ui.common

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Corner radius of pin cards in the grid; the detail image morphs from it to square corners. */
val PinCardCorner = 16.dp

/**
 * Links a pin's image between the grid and the detail screen, so it flies from one to the other.
 * [corner] is this side's corner radius; the detail screen animates its own from the card's to 0.
 */
@Composable
fun Modifier.sharedPinImage(pinId: Int, corner: Dp): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedContentScope.current
    return with(shared) {
        this@sharedPinImage.sharedBounds(
            sharedContentState = rememberSharedContentState("pin-image-$pinId"),
            animatedVisibilityScope = animated,
            clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(corner)),
        )
    }
}
