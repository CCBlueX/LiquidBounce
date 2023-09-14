package net.ccbluex.liquidbounce.utils.entity

import net.minecraft.util.math.Vec3d

interface PlayerSimulation {
    val pos: Vec3d

    fun tick()
}
