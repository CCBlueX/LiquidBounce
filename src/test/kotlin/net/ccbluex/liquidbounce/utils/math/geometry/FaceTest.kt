package net.ccbluex.liquidbounce.utils.math.geometry

import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.math.PI

class FaceTest {

    @Test
    fun nearestPoint() {
        val fromBaseVec = Vec3d(0.0, 0.0, 0.0)
        val toBaseVec = Vec3d(1.0, 1.0, 0.0)

        rotForEach2D { rotationY, rotationZ ->
            val toVec = toBaseVec.rotateY(rotationY).rotateY(rotationZ)

            val face = Face(fromBaseVec, toVec)

            val normalVec = face.toPlane().normalVec

            val fromPos = face.center.add(normalVec.multiply(5.0))

            assertVecEquals(
                face.center, face.nearestPointTo(
                    Line(
                        fromPos,
                        normalVec.negate()
                    )
                )!!
            )

            rotForEach1D { rot ->
                val otherPos = Vec3d(face.to.x * 0.5, 0.0, 0.0).rotateX(rot)

                val line = Line(fromPos, fromPos.subtract(otherPos.multiply(1.5).add(face.center)))

                val b = face.nearestPointTo(line)!!

                assertVecEquals(otherPos.add(face.center), b)
            }
        }
    }

    private fun rotForEach2D(fn: (Float, Float) -> Unit) {
        for (rotY in -2 until 2) {
            for (rotZ in -2 until 2) {
                val rotationY = rotY / 2.0F * PI.toFloat()
                val rotationZ = rotZ / 2.0F * PI.toFloat()

                fn(rotationY, rotationZ)
            }
        }
    }

    private fun rotForEach1D(fn: (Float) -> Unit) {
        for (rot in -2 until 2) {
            fn(rot / 2.0F * PI.toFloat())
        }
    }

    fun assertVecEquals(a: Vec3d, b: Vec3d) {
        if (!MathHelper.approximatelyEquals(a.subtract(b).lengthSquared(), 0.0)) {
            assertEquals(a, b)
        }
    }
}
