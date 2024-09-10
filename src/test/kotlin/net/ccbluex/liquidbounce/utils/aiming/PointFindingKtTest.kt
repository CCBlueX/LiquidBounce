package net.ccbluex.liquidbounce.utils.aiming

import net.minecraft.util.math.Vec3d
import org.joml.Matrix3f
import org.junit.jupiter.api.Test
import kotlin.math.atan2
import kotlin.math.hypot

class PointFindingKtTest {

    @Test
    fun testPlanePointConstruction() {
        val normalVec = Vec3d(-1.0, 1.0, -1.0).normalize()

        val hypotenuse = hypot(normalVec.x, normalVec.z)

        val yawAtan = atan2(normalVec.z, normalVec.x).toFloat()
        val pitchAtan = atan2(normalVec.y, hypotenuse).toFloat()

        val initVec = Vec3d(1.0, 0.0, 0.0)
        val rotZ = initVec.rotateZ(-pitchAtan)
        val rotY = rotZ.rotateY(-yawAtan)

        val rotMatrix1 = Matrix3f().rotateZ(-pitchAtan)
        val rotMatrix2 = Matrix3f().rotateY(yawAtan)

        val totalMatrix = rotMatrix1.mul(rotMatrix2)

        println(rotY.dotProduct(normalVec))
        println(rotY)
        println(normalVec.toVector3f().mul(totalMatrix))
    }

}
