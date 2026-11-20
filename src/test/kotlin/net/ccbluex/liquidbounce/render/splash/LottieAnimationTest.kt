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

import net.ccbluex.liquidbounce.utils.math.cubicBezier
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LottieAnimationTest {

    private fun animation(layers: String, markers: String = "[]") = LottieAnimation.parse(
        """{"fr":60,"ip":0,"op":120,"w":1920,"h":1080,"assets":[],"layers":$layers,"markers":$markers}"""
    )

    private fun opacity(k: String) = animation("""[{"ty":3,"nm":"n","ks":{"o":{"a":1,"k":$k}}}]""")
        .layers.single().transform

    @Test
    fun `linear keyframes interpolate and hold outside their range`() {
        val transform = opacity("""[{"t":0,"s":[0]},{"t":10,"s":[100]}]""")
        assertEquals(0f, transform.opacity(-5f))
        assertEquals(0.5f, transform.opacity(5f), 1e-4f)
        assertEquals(1f, transform.opacity(50f))
    }

    @Test
    fun `hold keyframes jump at the next keyframe`() {
        val transform = opacity("""[{"t":0,"s":[20],"h":1},{"t":10,"s":[80]}]""")
        assertEquals(0.2f, transform.opacity(9.9f), 1e-4f)
        assertEquals(0.8f, transform.opacity(10f), 1e-4f)
    }

    @Test
    fun `keyframes use the bezier easing of their handles`() {
        val transform = opacity(
            """[{"t":0,"s":[0],"o":{"x":[0.16],"y":[1]},"i":{"x":[0.3],"y":[1]}},{"t":100,"s":[100]}]"""
        )
        // Ease-out: most of the way there after a quarter of the time
        assertTrue(transform.opacity(25f) > 0.8f)
        assertEquals(cubicBezier(0.42f, 0f, 0.58f, 1f, 0.5f), 0.5f, 1e-3f)
        assertEquals(cubicBezier(0f, 0f, 1f, 1f, 0.3f), 0.3f, 1e-3f)
    }

    @Test
    fun `old exports keep the end value on the keyframe`() {
        val transform = opacity("""[{"t":0,"s":[0],"e":[40]},{"t":10}]""")
        assertEquals(0.2f, transform.opacity(5f), 1e-4f)
        assertEquals(0.4f, transform.opacity(30f), 1e-4f)
    }

    @Test
    fun `transforms scale and rotate around the anchor`() {
        val transform = animation(
            """[{"ty":3,"nm":"n","ks":{"a":{"k":[10,10]},"p":{"k":[100,50]},"s":{"k":[200,200]},"r":{"k":90}}}]"""
        ).layers.single().transform

        val matrix = transform.matrix(0f, Matrix3x2f())
        val anchor = matrix.transformPosition(10f, 10f, Vector2f())
        assertEquals(100f, anchor.x, 1e-3f)
        assertEquals(50f, anchor.y, 1e-3f)

        // One unit right of the anchor ends up two units further along the rotated x axis, which points down
        val right = matrix.transformPosition(11f, 10f, Vector2f())
        assertEquals(100f, right.x, 1e-3f)
        assertEquals(52f, right.y, 1e-3f)
    }

    @Test
    fun `split positions read each dimension on its own`() {
        val transform = animation(
            """[{"ty":3,"nm":"n","ks":{"p":{"s":true,"x":{"k":3},""" +
                """"y":{"a":1,"k":[{"t":0,"s":[0]},{"t":10,"s":[10]}]}}}}]"""
        ).layers.single().transform

        val position = transform.matrix(5f, Matrix3x2f()).transformPosition(0f, 0f, Vector2f())
        assertEquals(3f, position.x, 1e-4f)
        assertEquals(5f, position.y, 1e-4f)
    }

    @Test
    fun `marker names come from plain and After Effects style comments`() {
        val markers = animation(
            "[]",
            """[{"tm":0,"cm":"intro","dr":30},{"tm":30,"cm":"{\"name\":\"outro\",\"other\":1}","dr":10}]"""
        ).markers

        assertEquals(listOf("intro", "outro"), markers.map(LottieMarker::name))
        assertEquals(40f, markers[1].end)
    }

    @Test
    fun `unsupported layers are skipped and reported`() {
        val animation = animation(
            """[{"ty":5,"nm":"text"},{"ty":1,"nm":"solid","sc":"#ff0000","sw":10,"sh":10,"ks":{}}]"""
        )

        val solid = assertIs<LottieContent.Solid>(animation.layers.single().content)
        assertEquals(0xFF0000, solid.color and 0xFFFFFF)
        assertTrue("text layers" in animation.unsupported)
    }

    @Test
    fun `rates, sizes and stretches that would divide by zero fall back to the defaults`() {
        val animation = LottieAnimation.parse(
            """{"fr":0,"w":0,"h":-1,"layers":[{"ty":3,"nm":"n","sr":0,"ks":{}}]}"""
        )
        assertEquals(60f, animation.frameRate)
        assertEquals(1920f, animation.width)
        assertEquals(1080f, animation.height)
        assertEquals(1f, animation.layers.single().timeStretch)
    }

    @Test
    fun `easing handles of different lengths or none at all still evaluate`() {
        val uneven = animation(
            """[{"ty":3,"nm":"n","ks":{"p":{"a":1,"k":[""" +
                """{"t":0,"s":[0,0],"o":{"x":[0.2,0.3],"y":[0,0]},"i":{"x":[0.8],"y":[1]}},{"t":10,"s":[100,100]}]}}}]"""
        ).layers.single().transform
        assertTrue(uneven.matrix(5f, Matrix3x2f()).m21() in 0f..100f)

        val empty = opacity("""[{"t":0,"s":[0],"o":{"x":[],"y":[]},"i":{"x":[],"y":[]}},{"t":10,"s":[100]}]""")
        assertEquals(0.5f, empty.opacity(5f), 1e-4f)
    }

}
