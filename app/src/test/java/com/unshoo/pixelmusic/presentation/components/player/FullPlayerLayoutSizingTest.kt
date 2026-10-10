package com.unshoo.pixelmusic.presentation.components.player

import androidx.compose.ui.unit.dp
import com.unshoo.pixelmusic.data.preferences.CarouselStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FullPlayerLayoutSizingTest {
    @Test
    fun widePortraitCapsArtworkToKeepControlsAndLowerActionsVisible() {
        val height = 800.dp
        val noPeekHeight = calculateFullPlayerCarouselHeight(
            maxWidth = 680.dp,
            maxHeight = height,
            carouselStyle = CarouselStyle.NO_PEEK,
            isLandscape = false
        )
        val onePeekHeight = calculateFullPlayerCarouselHeight(
            maxWidth = 680.dp,
            maxHeight = height,
            carouselStyle = CarouselStyle.ONE_PEEK,
            isLandscape = false
        )

        assertEquals(420.dp, noPeekHeight)
        assertEquals(420.dp, onePeekHeight)
        assertTrue(noPeekHeight <= height - 380.dp)
    }

    @Test
    fun narrowPortraitKeepsNaturalNoPeekArtworkSize() {
        assertEquals(
            360.dp,
            calculateFullPlayerCarouselHeight(
                maxWidth = 360.dp,
                maxHeight = 800.dp,
                carouselStyle = CarouselStyle.NO_PEEK,
                isLandscape = false
            )
        )
    }

    @Test
    fun landscapeArtworkRespectsAvailableHeight() {
        assertEquals(
            288.dp,
            calculateFullPlayerCarouselHeight(
                maxWidth = 600.dp,
                maxHeight = 320.dp,
                carouselStyle = CarouselStyle.NO_PEEK,
                isLandscape = true
            )
        )
    }
}
