/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityTNTPrimed
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityTNTPrimed
import net.minecraft.entity.player.EntityPlayer
import java.util.*

open class EntityImpl<out T : Entity>(val wrapped: T) : IEntity
{
	// distanceWalked
	override var distanceWalkedOnStepModified: Float
		get() = wrapped.distanceWalkedOnStepModified
		set(value)
		{
			wrapped.distanceWalkedOnStepModified = value
		}
	override var distanceWalkedModified: Float
		get() = wrapped.distanceWalkedModified
		set(value)
		{
			wrapped.distanceWalkedModified = value
		}

	// Step Height
	override var stepHeight: Float
		get() = wrapped.stepHeight
		set(value)
		{
			wrapped.stepHeight = value
		}

	override val horizontalFacing: IEnumFacing
		get() = wrapped.horizontalFacing.wrap()

	override val lookVec: WVec3?
		get() = wrapped.lookVec.wrap()

	override var isDead: Boolean
		get() = wrapped.isDead
		set(value)
		{
			wrapped.isDead = value
		}

	// collision check
	override val isCollidedVertically: Boolean
		get() = wrapped.isCollidedVertically
	override val isCollidedHorizontally: Boolean
		get() = wrapped.isCollidedHorizontally

	override var isAirBorne: Boolean
		get() = wrapped.isAirBorne
		set(value)
		{
			wrapped.isAirBorne = value
		}

	override val hurtResistantTime: Int
		get() = wrapped.hurtResistantTime
	override var noClip: Boolean
		get() = wrapped.noClip
		set(value)
		{
			wrapped.noClip = value
		}

	override var sprinting: Boolean
		get() = wrapped.isSprinting
		set(value)
		{
			wrapped.isSprinting = value
		}

	override val sneaking: Boolean
		get() = wrapped.isSneaking

	override val positionVector: WVec3
		get() = wrapped.positionVector.wrap()

	override val isRiding: Boolean
		get() = wrapped.isRiding

	override val position: WBlockPos
		get() = wrapped.position.wrap()

	override val burning: Boolean
		get() = wrapped.isBurning

	override var fallDistance: Float
		get() = wrapped.fallDistance
		set(value)
		{
			wrapped.fallDistance = value
		}
	override val isInWater: Boolean
		get() = wrapped.isInWater
	override var isInWeb: Boolean
		get() = wrapped.isInWeb
		set(value)
		{
			wrapped.isInWeb = value
		}
	override val isInLava: Boolean
		get() = wrapped.isInLava

	override val width: Float
		get() = wrapped.width
	override val height: Float
		get() = wrapped.height

	override var onGround: Boolean
		get() = wrapped.onGround
		set(value)
		{
			wrapped.onGround = value
		}
	override val ridingEntity: IEntity?
		get() = wrapped.ridingEntity?.wrap()
	override val collisionBorderSize: Float
		get() = wrapped.collisionBorderSize
	override var motionX: Double
		get() = wrapped.motionX
		set(value)
		{
			wrapped.motionX = value
		}
	override var motionY: Double
		get() = wrapped.motionY
		set(value)
		{
			wrapped.motionY = value
		}
	override var motionZ: Double
		get() = wrapped.motionZ
		set(value)
		{
			wrapped.motionZ = value
		}
	override val eyeHeight: Float
		get() = wrapped.eyeHeight
	override var entityBoundingBox: IAxisAlignedBB
		get() = wrapped.entityBoundingBox.wrap()
		set(value)
		{
			wrapped.entityBoundingBox = value.unwrap()
		}
	override var posX: Double
		get() = wrapped.posX
		set(value)
		{
			wrapped.posX = value
		}

	override var posY: Double
		get() = wrapped.posY
		set(value)
		{
			wrapped.posY = value
		}
	override var posZ: Double
		get() = wrapped.posZ
		set(value)
		{
			wrapped.posZ = value
		}
	override val lastTickPosX: Double
		get() = wrapped.lastTickPosX
	override val lastTickPosY: Double
		get() = wrapped.lastTickPosY
	override val lastTickPosZ: Double
		get() = wrapped.lastTickPosZ
	override val prevPosX: Double
		get() = wrapped.prevPosX
	override val prevPosY: Double
		get() = wrapped.prevPosY
	override val prevPosZ: Double
		get() = wrapped.prevPosZ
	override var rotationYaw: Float
		get() = wrapped.rotationYaw
		set(value)
		{
			wrapped.rotationYaw = value
		}
	override var rotationPitch: Float
		get() = wrapped.prevRotationPitch
		set(value)
		{
			wrapped.rotationPitch = value
		}
	override val entityId: Int
		get() = wrapped.entityId
	override val displayName: IIChatComponent
		get() = wrapped.displayName.wrap()
	override val uniqueID: UUID
		get() = wrapped.uniqueID
	override val name: String
		get() = wrapped.name
	override val ticksExisted: Int
		get() = wrapped.ticksExisted
	override val entityAlive: Boolean
		get() = wrapped.isEntityAlive
	override val invisible: Boolean
		get() = wrapped.isInvisible

	override val prevRotationYaw: Float
		get() = wrapped.prevRotationYaw

	/* Type casting */
	override fun asEntityPlayer(): IEntityPlayer = EntityPlayerImpl(wrapped as EntityPlayer)

	override fun asEntityLivingBase(): IEntityLivingBase = EntityLivingBaseImpl(wrapped as EntityLivingBase)

	override fun asEntityTNTPrimed(): IEntityTNTPrimed = EntityTNTPrimedImpl(wrapped as EntityTNTPrimed)

	/* Position-related */
	override fun getPositionEyes(partialTicks: Float): WVec3 = wrapped.getPositionEyes(partialTicks).wrap()

	override fun moveEntity(x: Double, y: Double, z: Double) = wrapped.moveEntity(x, y, z)

	override fun copyLocationAndAnglesFrom(player: IEntityPlayerSP) = wrapped.copyLocationAndAnglesFrom(player.unwrap())

	override fun getLook(partialTicks: Float): WVec3 = wrapped.getLook(partialTicks).wrap()

	override fun setPosition(x: Double, y: Double, z: Double) = wrapped.setPosition(x, y, z)
	override fun setPositionAndUpdate(posX: Double, posY: Double, posZ: Double) = wrapped.setPositionAndUpdate(posX, posY, posZ)
	override fun setPositionAndRotation(oldX: Double, oldY: Double, oldZ: Double, rotationYaw: Float, rotationPitch: Float) = wrapped.setPositionAndRotation(oldX, oldY, oldZ, rotationYaw, rotationPitch)

	/* Distance */
	override fun getDistanceToEntity(it: IEntity): Float = wrapped.getDistanceToEntity(it.unwrap())
	override fun getDistanceSqToEntity(it: IEntity): Double = wrapped.getDistanceSqToEntity(it.unwrap())

	override fun getDistance(x: Double, y: Double, z: Double): Double = wrapped.getDistance(x, y, z)

	override fun getDistanceSq(blockPos: WBlockPos): Double = wrapped.getDistanceSq(blockPos.unwrap())
	override fun getDistanceSq(x: Double, y: Double, z: Double): Double = wrapped.getDistanceSq(x, y, z)

	override fun rayTrace(range: Double, partialTicks: Float): IMovingObjectPosition? = wrapped.rayTrace(range, partialTicks).wrap()

	override fun isInsideOfMaterial(material: IMaterial): Boolean = wrapped.isInsideOfMaterial(material.unwrap())

	override fun canBeCollidedWith(): Boolean = wrapped.canBeCollidedWith()

	override fun canRiderInteract(): Boolean = wrapped.canRiderInteract()

	override fun equals(other: Any?): Boolean = other is EntityImpl<*> && wrapped.isEntityEqual(other.unwrap())
}

fun IEntity.unwrap(): Entity = (this as EntityImpl<*>).wrapped
fun Entity.wrap(): IEntity = EntityImpl(this)
