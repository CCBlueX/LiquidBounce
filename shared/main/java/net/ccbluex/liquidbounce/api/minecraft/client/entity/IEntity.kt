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
interface IEntity
{
	// <editor-fold desc="DistanceWalked">
	var distanceWalkedOnStepModified: Float
	var distanceWalkedModified: Float
	// </editor-fold>

	// <editor-fold desc="States">
	var isDead: Boolean
	val isCollidedVertically: Boolean
	val isCollidedHorizontally: Boolean
	var isAirBorne: Boolean
	var noClip: Boolean

	val isInWater: Boolean
	var isInWeb: Boolean
	val isInLava: Boolean
	val isHandActive: Boolean
	val isSilent: Boolean

	@get:JvmName("isSprinting")
	var sprinting: Boolean

	@get:JvmName("isSneaking")
	val sneaking: Boolean

	@get:JvmName("isRiding")
	val isRiding: Boolean

	@get:JvmName("isBurning")
	val burning: Boolean

	@get:JvmName("isEntityAlive")
	val entityAlive: Boolean

	@get:JvmName("isInvisible")
	val invisible: Boolean
	// </editor-fold>

	// <editor-fold desc="Position & Movement">
	var stepHeight: Float

	var motionX: Double
	var motionY: Double
	var motionZ: Double
	var onGround: Boolean

	val position: WBlockPos
	val positionVector: WVec3

	var posX: Double
	var posY: Double
	var posZ: Double

	val serverPosX: Long
	val serverPosY: Long
	val serverPosZ: Long

	val lastTickPosX: Double
	val lastTickPosY: Double
	val lastTickPosZ: Double

	val prevPosX: Double
	val prevPosY: Double
	val prevPosZ: Double
	// </editor-fold>

	// <editor-fold desc="Rotation">
	var rotationYaw: Float
	var rotationPitch: Float

	var prevRotationYaw: Float
	var prevRotationPitch: Float

	val horizontalFacing: IEnumFacing
	val lookVec: WVec3?
	// </editor-fold>

	// <editor-fold desc="Border">
	val width: Float
	val height: Float
	val eyeHeight: Float
	val collisionBorderSize: Float
	var entityBoundingBox: IAxisAlignedBB
	// </editor-fold>

	val entityId: Int
	val displayName: IIChatComponent
	val uniqueID: UUID
	val name: String

	val ticksExisted: Int
	var fallDistance: Float
	val hurtResistantTime: Int
	val air: Int

	val ridingEntity: IEntity?

	// <editor-fold desc="Type casting">
	fun asEntityPlayer(): IEntityPlayer
	fun asEntityLivingBase(): IEntityLivingBase
	fun asEntityTNTPrimed(): IEntityTNTPrimed
	fun asEntityArrow(): IEntityArrow
	fun asEntityPotion(): IEntityPotion
	fun asEntityFishHook(): IEntityFishHook
	fun asEntityCreeper(): IEntityCreeper
	// </editor-fold>

	// <editor-fold desc="Position & Movement">
	fun getPositionEyes(partialTicks: Float): WVec3
	fun getLook(partialTicks: Float): WVec3

	fun moveEntity(x: Double, y: Double, z: Double)
	fun setPosition(x: Double, y: Double, z: Double)
	fun setPositionAndRotation(posX: Double, posY: Double, posZ: Double, rotationYaw: Float, rotationPitch: Float)
	fun setPositionAndUpdate(posX: Double, posY: Double, posZ: Double)

	fun copyLocationAndAnglesFrom(player: IEntity)
	// </editor-fold>

	// <editor-fold desc="Distance">
	fun getDistance(x: Double, y: Double, z: Double): Double
	fun getDistanceToEntity(it: IEntity): Float

	fun getDistanceSq(blockPos: WBlockPos): Double
	fun getDistanceSq(x: Double, y: Double, z: Double): Double
	fun getDistanceSqToEntity(it: IEntity): Double
	// </editor-fold>

	fun canRiderInteract(): Boolean
	fun canBeCollidedWith(): Boolean

	fun setCanBeCollidedWith(value: Boolean)

	fun rayTrace(range: Double, partialTicks: Float): IMovingObjectPosition?
	fun isInsideOfMaterial(material: IMaterial): Boolean

	override operator fun equals(other: Any?): Boolean
}
