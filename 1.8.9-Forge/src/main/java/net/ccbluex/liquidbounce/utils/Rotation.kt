package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3

/**
 * Rotations
 */
data class Rotation(var yaw: Float, var pitch: Float) {

    /**
     * Set roations to [player]
     */
    fun toPlayer(player: EntityPlayer) {
        if(yaw.isNaN() || pitch.isNaN()) return

        player.rotationYaw = yaw
        player.rotationPitch = pitch
    }

    /**
     * Fix minecraft gcd
     */
    fun fixGcd() {
        // TODO: Currently in testing

        val sensitivity = 0.91F

        // val oldYaw = yaw
        // val oldPitch = pitch

        yaw -= yaw % sensitivity
        pitch -= pitch % sensitivity

        // ClientUtils.displayChatMessage("§7sen: §8$sensitivity §c- §7yaw: §8$oldYaw §c-> §8$yaw §c- §8pitch: §7$oldPitch §c-> §8$pitch")
    }

}

/**
 * Rotation with vector
 */
data class VecRotation(val vec: Vec3, val rotation: Rotation)

/**
 * Rotation with place info
 */
data class PlaceRotation(val placeInfo: PlaceInfo, val rotation: Rotation)