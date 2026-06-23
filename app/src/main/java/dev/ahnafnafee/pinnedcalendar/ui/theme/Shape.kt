package dev.ahnafnafee.pinnedcalendar.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon

/** Smoothness of the corner transition (0 = a plain rounded corner, 1 = maximally continuous). */
private const val DefaultSmoothing = 0.6f

/**
 * A rounded rectangle with continuous-curvature ("squircle") corners, built on the same
 * androidx.graphics.shapes engine that backs Material 3 Expressive shapes. Compared with
 * [androidx.compose.foundation.shape.RoundedCornerShape] the corner flows into the edge instead of
 * meeting it at a circular arc — the softer silhouette used across the app's cards and chips.
 */
class SmoothRoundedCornerShape(
    private val radius: Dp,
    private val smoothing: Float = DefaultSmoothing,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.minDimension <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        val px = with(density) { radius.toPx() }.coerceIn(0f, size.minDimension / 2f)
        val polygon = RoundedPolygon(
            vertices = floatArrayOf(
                0f, 0f,
                size.width, 0f,
                size.width, size.height,
                0f, size.height,
            ),
            rounding = CornerRounding(px, smoothing),
        )
        return Outline.Generic(polygon.toPath())
    }

    private fun RoundedPolygon.toPath(): Path {
        val path = Path()
        val cubics = cubics
        if (cubics.isEmpty()) return path
        path.moveTo(cubics.first().anchor0X, cubics.first().anchor0Y)
        cubics.forEach { c -> path.cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y) }
        path.close()
        return path
    }
}

/** Cached smooth shapes for direct use on specific surfaces (instances are reused, not rebuilt). */
object AppShape {
    val card = SmoothRoundedCornerShape(28.dp)
    val cardLarge = SmoothRoundedCornerShape(32.dp)
    val chip = SmoothRoundedCornerShape(16.dp)
    val field = SmoothRoundedCornerShape(20.dp)
    val pill = SmoothRoundedCornerShape(50.dp)
    val bar = SmoothRoundedCornerShape(8.dp)
}
