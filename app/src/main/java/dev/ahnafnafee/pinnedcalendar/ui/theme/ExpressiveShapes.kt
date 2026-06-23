package dev.ahnafnafee.pinnedcalendar.ui.theme

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star

// Unit polygons (centred at the origin, radius 1) used as morph endpoints for expressive accents.
private val CircleAccent = RoundedPolygon.circle(numVertices = 12)
private val CookieAccent = RoundedPolygon.star(
    numVerticesPerRadius = 12,
    innerRadius = 0.82f,
    rounding = CornerRounding(0.20f),
)

/** A plain disc that blooms into a scalloped "cookie" — used to mark the selected accent swatch. */
val CircleToCookieMorph: Morph = Morph(CircleAccent, CookieAccent)

/**
 * Outlines [morph] at [percentage] (0 = start shape, 1 = end shape), scaled to fill the component.
 * Drive [percentage] with an animated value to animate the morph on selection.
 */
class MorphableShape(private val morph: Morph, private val percentage: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.minDimension <= 0f) return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        return Outline.Generic(morph.asCubics(percentage.coerceIn(0f, 1f)).toScaledPath(size))
    }

    private fun List<Cubic>.toScaledPath(size: Size): Path {
        val path = Path()
        if (isEmpty()) return path
        // Endpoint polygons span [-1, 1]; map that into the component's [0, size] box.
        fun mapX(x: Float) = (x + 1f) / 2f * size.width
        fun mapY(y: Float) = (y + 1f) / 2f * size.height
        val first = first()
        path.moveTo(mapX(first.anchor0X), mapY(first.anchor0Y))
        forEach { c -> path.cubicTo(mapX(c.control0X), mapY(c.control0Y), mapX(c.control1X), mapY(c.control1Y), mapX(c.anchor1X), mapY(c.anchor1Y)) }
        path.close()
        return path
    }
}
