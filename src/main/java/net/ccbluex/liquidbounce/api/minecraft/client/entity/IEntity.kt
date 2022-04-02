/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.entity

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.*
import java.util.*

@Suppress("INAPPLICABLE_JVM_NAME")
interface IEntity {
    var distanceWalkedOnStepModified: Float
    var distanceWalkedModified: Float

    @get:JvmName("isSneaking")
    val sneaking: Boolean
    var stepHeight: Float
    val horizontalFacing: IEnumFacing
    val lookVec: WVec3?
    var isDead: Boolean
    val isCollidedVertically: Boolean
    val isCollidedHorizontally: Boolean
    var isAirBorne: Boolean
    val hurtResistantTime: Int
    var noClip: Boolean
    var sprinting: Boolean
    val positionVector: WVec3

    @get:JvmName("isRiding")
    val isRiding: Boolean
    val position: WBlockPos

    @get:JvmName("isBurning")
    val burning: Boolean
    var fallDistance: Float
    val isInWater: Boolean
    var isInWeb: Boolean
    val isInLava: Boolean
    val width: Float
    val height: Float
    var onGround: Boolean
    val ridingEntity: IEntity?
    val collisionBorderSize: Float
    var motionX: Double
    var motionY: Double
    var motionZ: Double

    val eyeHeight: Float
    var entityBoundingBox: IAxisAlignedBB
    val posX: Double
    var posY: Double
    val posZ: Double

    val lastTickPosX: Double
    val lastTickPosY: Double
    val lastTickPosZ: Double

    val prevPosX: Double
    val prevPosY: Double
    val prevPosZ: Double

    var rotationYaw: Float
    var rotationPitch: Float
    val entityId: Int
    val displayName: IIChatComponent?
    val uniqueID: UUID
    val name: String?

    val ticksExisted: Int

    @get:JvmName("isEntityAlive")
    val entityAlive: Boolean

    @get:JvmName("isInvisible")
    val invisible: Boolean

    fun getPositionEyes(partialTicks: Float): WVec3

    fun canBeCollidedWith(): Boolean
    fun canRiderInteract(): Boolean
    fun moveEntity(x: Double, y: Double, z: Double)
    fun getDistanceToEntity(it: IEntity): Float
    fun getDistanceSqToEntity(it: IEntity): Double

    fun asEntityPlayer(): IEntityPlayer
    fun asEntityLivingBase(): IEntityLivingBase
    fun asEntityTNTPrimed(): IEntityTNTPrimed

    fun getDistance(x: Double, y: Double, z: Double): Double
    fun setPosition(x: Double, y: Double, z: Double)

    fun getDistanceSq(blockPos: WBlockPos): Double
    fun setPositionAndUpdate(posX: Double, posY: Double, posZ: Double)
    fun rayTrace(range: Double, partialTicks: Float): IMovingObjectPosition?
    fun getLook(partialTicks: Float): WVec3
    fun isInsideOfMaterial(material: IMaterial): Boolean
    fun copyLocationAndAnglesFrom(player: IEntityPlayerSP)
    fun setPositionAndRotation(oldX: Double, oldY: Double, oldZ: Double, rotationYaw: Float, rotationPitch: Float)
}