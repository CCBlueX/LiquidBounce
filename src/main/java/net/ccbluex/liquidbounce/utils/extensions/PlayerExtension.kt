/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.file.FileManager.friendsConfig
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.getFixedSensitivityAngle
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.entity.monster.EntityGhast
import net.minecraft.entity.monster.EntityGolem
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.passive.EntityBat
import net.minecraft.entity.passive.EntitySquid
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3

/**
 * Allows to get the distance between the current entity and [entity] from the nearest corner of the bounding box
 */
fun Entity.getDistanceToEntityBox(entity: Entity) = eyes.distanceTo(getNearestPointBB(eyes, entity.hitBox))

fun Entity.getDistanceToBox(box: AxisAlignedBB) = eyes.distanceTo(getNearestPointBB(eyes, box))

fun getNearestPointBB(eye: Vec3, box: AxisAlignedBB): Vec3 {
    val origin = doubleArrayOf(eye.xCoord, eye.yCoord, eye.zCoord)
    val destMins = doubleArrayOf(box.minX, box.minY, box.minZ)
    val destMaxs = doubleArrayOf(box.maxX, box.maxY, box.maxZ)
    for (i in 0..2) {
        if (origin[i] > destMaxs[i]) origin[i] = destMaxs[i] else if (origin[i] < destMins[i]) origin[i] = destMins[i]
    }
    return Vec3(origin[0], origin[1], origin[2])
}

fun EntityPlayer.getPing() = mc.netHandler.getPlayerInfo(uniqueID)?.responseTime ?: 0

fun Entity.isAnimal() =
    this is EntityAnimal
        || this is EntitySquid
        || this is EntityGolem
        || this is EntityBat

fun Entity.isMob() =
    this is EntityMob
        || this is EntityVillager
        || this is EntitySlime
        || this is EntityGhast
        || this is EntityDragon

fun EntityPlayer.isClientFriend(): Boolean {
    val entityName = name ?: return false

    return friendsConfig.isFriend(stripColor(entityName))
}

val Entity?.rotation
    get() = Rotation(this?.rotationYaw ?: 0f, this?.rotationPitch ?: 0f)

val Entity.hitBox: AxisAlignedBB
    get() {
        val borderSize = collisionBorderSize.toDouble()
        return entityBoundingBox.expand(borderSize, borderSize, borderSize)
    }

val Entity.eyes: Vec3
    get() = getPositionEyes(1f)

/**
 * Setting yaw to a fixed sensitivity angle
 */

fun EntityPlayerSP.setFixedSensitivityAngles(yaw: Float? = null, pitch: Float? = null) {
    if (yaw != null) fixedSensitivityYaw = yaw

    if (pitch != null) fixedSensitivityPitch = pitch
}

var EntityPlayerSP.fixedSensitivityYaw
    get() = getFixedSensitivityAngle(mc.thePlayer.rotationYaw)
    set(yaw) {
        rotationYaw = getFixedSensitivityAngle(yaw, rotationYaw)
    }

var EntityPlayerSP.fixedSensitivityPitch
    get() = getFixedSensitivityAngle(rotationPitch)
    set(pitch) {
        rotationPitch = getFixedSensitivityAngle(pitch.coerceIn(-90f, 90f), rotationPitch)
    }

// Makes fixedSensitivityYaw, ... += work
operator fun EntityPlayerSP.plusAssign(value: Float) {
    fixedSensitivityYaw += value
    fixedSensitivityPitch += value
}

fun EntityPlayerSP.stopXZ() {
    motionX = 0.0
    motionZ = 0.0
}

fun EntityPlayerSP.stop() {
    stopXZ()
    motionY = 0.0
}