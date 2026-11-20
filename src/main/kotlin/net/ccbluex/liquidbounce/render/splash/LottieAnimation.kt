/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.render.splash

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.config.gson.util.array
import net.ccbluex.liquidbounce.config.gson.util.boolean
import net.ccbluex.liquidbounce.config.gson.util.float
import net.ccbluex.liquidbounce.config.gson.util.obj
import net.ccbluex.liquidbounce.config.gson.util.string
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.math.cubicBezier
import org.joml.Matrix3x2f

/**
 * The part of the Lottie format the splash screen plays: image, solid, null, precomp and shape layers
 * (rectangles, ellipses, fills), transforms with bezier easing, parenting, masks as rectangles and markers.
 *
 * Everything else (text layers, effects, expressions, track mattes, paths) is skipped and listed in
 * [unsupported].
 */
class LottieAnimation(
    val frameRate: Float,
    val inPoint: Float,
    val outPoint: Float,
    val width: Float,
    val height: Float,
    val layers: List<LottieLayer>,
    val images: List<LottieImage>,
    val markers: List<LottieMarker>,
    val unsupported: Set<String>,
) {

    fun marker(name: String) = markers.find { marker -> marker.name == name }

    companion object {

        const val MAX_LAYERS = 256

        fun parse(json: String): LottieAnimation = LottieParser().animation(JsonParser.parseString(json).asJsonObject)

    }

}

/**
 * @property path relative to the animation file (`u` + `p`), or a `data:` URI for embedded images
 */
class LottieImage(val id: String, val width: Int, val height: Int, val path: String)

class LottieMarker(val name: String, val frame: Float, val duration: Float) {
    val end get() = frame + duration
}

class LottieLayer(
    val index: Int?,
    val name: String,
    val parent: Int?,
    val inPoint: Float,
    val outPoint: Float,
    val startTime: Float,
    val timeStretch: Float,
    val hidden: Boolean,
    val transform: LottieTransform,
    val content: LottieContent,
    val masks: List<LottieProperty>,
) {

    fun isVisibleAt(frame: Float) = !hidden && frame >= inPoint && frame < outPoint

    /** Scratch for the render thread, the layer's matrix within its composition. */
    internal val world = Matrix3x2f()
    internal var worldStamp = -1

}

sealed interface LottieContent {
    object Empty : LottieContent
    class Image(val imageId: String) : LottieContent
    class Solid(val color: Int, val width: Float, val height: Float) : LottieContent
    class Precomp(val layers: List<LottieLayer>) : LottieContent
    class Shapes(val items: List<LottieShape>) : LottieContent
}

sealed interface LottieShape {
    class Group(val transform: LottieTransform, val items: List<LottieShape>) : LottieShape
    class Rect(val position: LottieProperty, val size: LottieProperty, val roundness: LottieProperty) : LottieShape
    class Ellipse(val position: LottieProperty, val size: LottieProperty) : LottieShape
    /** Fills the rectangles and ellipses of its group; [color] is RGBA in 0..1. */
    class Fill(val color: LottieProperty, val opacity: LottieProperty) : LottieShape
}

class LottieTransform(
    private val anchor: LottieProperty,
    private val position: LottieProperty,
    private val positionX: LottieProperty?,
    private val positionY: LottieProperty?,
    private val scale: LottieProperty,
    private val rotation: LottieProperty,
    private val opacity: LottieProperty,
) {

    fun anchorX(frame: Float) = anchor.value(frame, 0)
    fun anchorY(frame: Float) = anchor.value(frame, 1)

    /**
     * Writes `translate(position) * rotate * scale * translate(-anchor)` into [out].
     *
     * @param scaleX extra horizontal scale around the anchor
     */
    fun matrix(frame: Float, out: Matrix3x2f, scaleX: Float = 1f): Matrix3x2f {
        val x = positionX?.value(frame, 0) ?: position.value(frame, 0)
        val y = positionY?.value(frame, 0) ?: position.value(frame, 1)
        return out.translation(x, y)
            .rotate(Math.toRadians(rotation.value(frame, 0).toDouble()).toFloat())
            .scale(scale.value(frame, 0) / 100f * scaleX, scale.value(frame, 1) / 100f)
            .translate(-anchorX(frame), -anchorY(frame))
    }

    fun opacity(frame: Float) = (opacity.value(frame, 0) / 100f).coerceIn(0f, 1f)

    companion object {
        val IDENTITY = LottieTransform(
            LottieProperty.of(0f, 0f), LottieProperty.of(0f, 0f), null, null,
            LottieProperty.of(100f, 100f), LottieProperty.of(0f), LottieProperty.of(100f),
        )
    }

}

/**
 * A static or keyframed value with any number of dimensions.
 */
class LottieProperty private constructor(
    private val static: FloatArray,
    private val keyframes: List<Keyframe>,
) {

    class Keyframe(
        val time: Float,
        val start: FloatArray,
        /** Old exports store the end value on the keyframe instead of the next one. */
        val end: FloatArray?,
        val hold: Boolean,
        val easeOut: FloatArray?,
        val easeIn: FloatArray?,
    )

    val dimensions get() = if (keyframes.isEmpty()) static.size else keyframes.first().start.size

    fun value(frame: Float, dimension: Int = 0): Float {
        if (keyframes.isEmpty()) {
            return static.getOrElse(dimension) { static.lastOrNull() ?: 0f }
        }

        val first = keyframes.first()
        if (frame <= first.time) {
            return first.start.at(dimension)
        }

        val index = keyframes.indexOfLast { keyframe -> keyframe.time <= frame }
        val current = keyframes[index]
        val next = keyframes.getOrNull(index + 1)
        if (current.hold || next == null) {
            return current.start.at(dimension)
        }

        val linear = ((frame - current.time) / (next.time - current.time)).coerceIn(0f, 1f)
        val eased = ease(current.easeOut, current.easeIn, dimension, linear)
        val from = current.start.at(dimension)
        return from + (next.start.at(dimension) - from) * eased
    }

    private fun FloatArray.at(dimension: Int) = getOrElse(dimension) { lastOrNull() ?: 0f }

    companion object {

        fun of(vararg values: Float) = LottieProperty(values, emptyList())

        internal fun animated(keyframes: List<Keyframe>): LottieProperty {
            // Old exports keep the end value on the keyframe and leave the start of the next one out
            val normalized = keyframes.mapIndexedNotNull { index, keyframe ->
                val start = keyframe.start.takeIf(FloatArray::isNotEmpty)
                    ?: keyframes.getOrNull(index - 1)?.end
                    ?: return@mapIndexedNotNull null
                Keyframe(keyframe.time, start, keyframe.end, keyframe.hold, keyframe.easeOut, keyframe.easeIn)
            }
            return LottieProperty(FloatArray(0), normalized)
        }

        /**
         * @param easeOut `[x, y]` pairs of the outgoing handle, one per dimension; null for linear
         */
        private fun ease(easeOut: FloatArray?, easeIn: FloatArray?, dimension: Int, x: Float): Float {
            if (easeOut == null || easeIn == null) {
                return x
            }

            val d = dimension.coerceAtMost(minOf(easeOut.size, easeIn.size) / 2 - 1)
            return cubicBezier(easeOut[d * 2], easeOut[d * 2 + 1], easeIn[d * 2], easeIn[d * 2 + 1], x)
        }

    }

}

private class LottieParser {

    private val unsupported = sortedSetOf<String>()
    private val precomps = HashMap<String, JsonArray>()
    private var layerCount = 0

    fun animation(json: JsonObject): LottieAnimation {
        val images = ArrayList<LottieImage>()
        json.array("assets")?.forEach { element ->
            val asset = element.asJsonObject
            val id = asset.string("id") ?: return@forEach
            when {
                asset.has("layers") -> precomps[id] = asset.array("layers") ?: JsonArray()
                asset.has("p") -> images += LottieImage(
                    id,
                    asset.float("w")?.toInt() ?: 0,
                    asset.float("h")?.toInt() ?: 0,
                    asset.string("p")!!.let { p -> if (p.startsWith("data:")) p else asset.string("u").orEmpty() + p },
                )
                else -> unsupported += "asset without image or layers"
            }
        }

        val markers = json.array("markers")?.map { element ->
            val marker = element.asJsonObject
            LottieMarker(
                markerName(marker.string("cm").orEmpty()),
                marker.float("tm") ?: 0f,
                marker.float("dr") ?: 0f,
            )
        }.orEmpty()

        return LottieAnimation(
            frameRate = json.positive("fr") ?: 60f,
            inPoint = json.float("ip") ?: 0f,
            outPoint = json.float("op") ?: 0f,
            width = json.positive("w") ?: 1920f,
            height = json.positive("h") ?: 1080f,
            layers = layers(json.array("layers") ?: JsonArray(), depth = 0),
            images = images,
            markers = markers,
            unsupported = unsupported,
        )
    }

    /** After Effects puts the comment of a marker as JSON into `cm` when it has more than a name. */
    private fun markerName(comment: String) = if (comment.trimStart().startsWith("{")) {
        runCatching { JsonParser.parseString(comment).asJsonObject.string("name") }.getOrNull() ?: comment
    } else {
        comment
    }.trim()

    private fun layers(array: JsonArray, depth: Int): List<LottieLayer> {
        require(depth < MAX_DEPTH) { "Precomps nest too deep" }
        return array.mapNotNull { element -> layer(element.asJsonObject, depth) }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun layer(json: JsonObject, depth: Int): LottieLayer? {
        require(++layerCount <= LottieAnimation.MAX_LAYERS) { "More than ${LottieAnimation.MAX_LAYERS} layers" }

        // Matte sources are only shown through the layer that uses them
        if (json.float("td") == 1f) {
            unsupported += "track mattes"
            return null
        }
        if (json.has("tt")) {
            unsupported += "track mattes"
        }
        if (json.array("ef")?.isEmpty == false) {
            unsupported += "effects"
        }

        val content = when (val type = json.float("ty")?.toInt()) {
            0 -> LottieContent.Precomp(
                precomps[json.string("refId")]?.let { layers(it, depth + 1) }.orEmpty()
            )
            1 -> LottieContent.Solid(
                runCatching { Color4b.fromHex(json.string("sc").orEmpty()).argb }.getOrDefault(0),
                json.float("sw") ?: 0f,
                json.float("sh") ?: 0f,
            )
            2 -> LottieContent.Image(json.string("refId").orEmpty())
            3 -> LottieContent.Empty
            4 -> LottieContent.Shapes(shapes(json.array("shapes") ?: JsonArray()))
            else -> {
                unsupported += if (type == 5) "text layers" else "layer type $type"
                return null
            }
        }

        val masks = json.array("masksProperties")?.mapNotNull { element ->
            val mask = element.asJsonObject
            if (mask.string("mode") != "a" || mask.boolean("inv") == true) {
                unsupported += "subtract, intersect and inverted masks"
                null
            } else {
                mask.obj("pt")?.let(::pathProperty)
            }
        }.orEmpty()

        return LottieLayer(
            index = json.float("ind")?.toInt(),
            name = json.string("nm").orEmpty(),
            parent = json.float("parent")?.toInt(),
            inPoint = json.float("ip") ?: 0f,
            outPoint = json.float("op") ?: Float.MAX_VALUE,
            startTime = json.float("st") ?: 0f,
            timeStretch = json.positive("sr") ?: 1f,
            hidden = json.boolean("hd") == true,
            transform = transform(json.obj("ks")),
            content = content,
            masks = masks,
        )
    }

    private fun shapes(array: JsonArray): List<LottieShape> = array.mapNotNull { element ->
        val shape = element.asJsonObject
        if (shape.boolean("hd") == true) {
            return@mapNotNull null
        }

        when (val type = shape.string("ty")) {
            "gr" -> {
                val items = shape.array("it") ?: JsonArray()
                val transform = items.firstOrNull { it.asJsonObject.string("ty") == "tr" }
                    ?.let { transform(it.asJsonObject) } ?: LottieTransform.IDENTITY
                LottieShape.Group(transform, shapes(items))
            }
            "rc" -> LottieShape.Rect(property(shape["p"]), property(shape["s"]), property(shape["r"], 0f))
            "el" -> LottieShape.Ellipse(property(shape["p"]), property(shape["s"]))
            "fl" -> LottieShape.Fill(property(shape["c"], 1f), property(shape["o"], 100f))
            "tr" -> null
            else -> {
                unsupported += "shape '$type'"
                null
            }
        }
    }

    private fun transform(json: JsonObject?): LottieTransform {
        json ?: return LottieTransform.IDENTITY
        val position = json.obj("p")
        val split = position?.boolean("s") == true
        return LottieTransform(
            anchor = property(json["a"], 0f),
            position = if (split) LottieProperty.of(0f, 0f) else property(position, 0f),
            positionX = if (split) property(position!!["x"], 0f) else null,
            positionY = if (split) property(position!!["y"], 0f) else null,
            scale = property(json["s"], 100f),
            rotation = property(json["r"] ?: json["rz"], 0f),
            opacity = property(json["o"], 100f),
        )
    }

    /**
     * @param default the value of a missing property, in every dimension
     */
    private fun property(element: JsonElement?, default: Float = 0f): LottieProperty {
        val json = element?.takeIf { it.isJsonObject }?.asJsonObject ?: return LottieProperty.of(default, default)
        if (json.has("x") && json.get("x").isJsonPrimitive && json.get("x").asJsonPrimitive.isString) {
            unsupported += "expressions"
        }

        val k = json["k"] ?: return LottieProperty.of(default, default)
        val keyframed = k.isJsonArray && k.asJsonArray.firstOrNull()?.isJsonObject == true
        if (!keyframed) {
            return LottieProperty.of(*numbers(k))
        }

        return LottieProperty.animated(k.asJsonArray.map { item ->
            val keyframe = item.asJsonObject
            LottieProperty.Keyframe(
                time = keyframe.float("t") ?: 0f,
                start = keyframe["s"]?.let(::numbers) ?: FloatArray(0),
                end = keyframe["e"]?.let(::numbers),
                hold = keyframe.float("h") == 1f,
                easeOut = keyframe.obj("o")?.let(::handle),
                easeIn = keyframe.obj("i")?.let(::handle),
            )
        })
    }

    /** A shape path, kept as its flattened vertices: masks only need their bounds. */
    private fun pathProperty(json: JsonObject): LottieProperty {
        val k = json["k"] ?: return LottieProperty.of()
        fun vertices(shape: JsonElement): FloatArray {
            val value = if (shape.isJsonArray) shape.asJsonArray.firstOrNull() else shape
            val v = value?.asJsonObject?.array("v") ?: return FloatArray(0)
            return v.flatMap { point -> point.asJsonArray.take(2).map { it.asFloat } }.toFloatArray()
        }

        if (k.isJsonObject) {
            return LottieProperty.of(*vertices(k))
        }

        return LottieProperty.animated(k.asJsonArray.map { item ->
            val keyframe = item.asJsonObject
            LottieProperty.Keyframe(
                time = keyframe.float("t") ?: 0f,
                start = keyframe["s"]?.let(::vertices) ?: FloatArray(0),
                end = keyframe["e"]?.let(::vertices),
                hold = keyframe.float("h") == 1f,
                easeOut = keyframe.obj("o")?.let(::handle),
                easeIn = keyframe.obj("i")?.let(::handle),
            )
        })
    }

    /** Bezier handle `{x, y}` as `[x0, y0, x1, y1, ...]`, one pair per dimension; null for linear. */
    private fun handle(json: JsonObject): FloatArray? {
        val x = numbers(json["x"] ?: return null)
        val y = numbers(json["y"] ?: return null)
        if (x.isEmpty() || y.isEmpty()) {
            return null
        }
        return FloatArray(maxOf(x.size, y.size) * 2) { i ->
            val d = i / 2
            if (i % 2 == 0) x.getOrElse(d) { x.last() } else y.getOrElse(d) { y.last() }
        }
    }

    private fun numbers(element: JsonElement): FloatArray = when {
        element.isJsonArray -> element.asJsonArray.filter { it.isJsonPrimitive }.map { it.asFloat }.toFloatArray()
        element.isJsonPrimitive -> floatArrayOf(element.asFloat)
        else -> FloatArray(0)
    }

    /** Rates, sizes and stretches divide; anything but a positive number falls back to the default. */
    private fun JsonObject.positive(key: String) = float(key)?.takeIf { it > 0f }

    private companion object {
        const val MAX_DEPTH = 8
    }

}
