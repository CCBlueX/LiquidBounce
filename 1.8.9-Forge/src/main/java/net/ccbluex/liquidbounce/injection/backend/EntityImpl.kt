/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.client.entity.*
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.*
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntityLivingBase
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityTNTPrimed
import net.minecraft.entity.monster.EntityCreeper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.entity.projectile.EntityPotion
import java.util.*

open class EntityImpl<out T : Entity>(val wrapped: T) : IEntity
{
	// <editor-fold desc="DistanceWalked">
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
	// </editor-fold>

	// <editor-fold desc="States">
	override var isDead: Boolean
		get() = wrapped.isDead
		set(value)
		{
			wrapped.isDead = value
		}

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

	override var noClip: Boolean
		get() = wrapped.noClip
		set(value)
		{
			wrapped.noClip = value
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
	override val isEating: Boolean
		get() = wrapped.isEating
	override val isSilent: Boolean
		get() = wrapped.isSilent

	override var sprinting: Boolean
		get() = wrapped.isSprinting
		set(value)
		{
			wrapped.isSprinting = value
		}
	override val sneaking: Boolean
		get() = wrapped.isSneaking
	override val isRiding: Boolean
		get() = wrapped.isRiding
	override val burning: Boolean
		get() = wrapped.isBurning
	override val entityAlive: Boolean
		get() = wrapped.isEntityAlive
	override val invisible: Boolean
		get() = wrapped.isInvisible
	// </editor-fold>

	// <editor-fold desc="Position & Movement">
	override var stepHeight: Float
		get() = wrapped.stepHeight
		set(value)
		{
			wrapped.stepHeight = value
		}

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
	override var onGround: Boolean
		get() = wrapped.onGround
		set(value)
		{
			wrapped.onGround = value
		}

	override val position: WBlockPos
		get() = wrapped.position.wrap()
	override val positionVector: WVec3
		get() = wrapped.positionVector.wrap()

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

	override val serverPosX: Int
		get() = wrapped.serverPosX
	override val serverPosY: Int
		get() = wrapped.serverPosY
	override val serverPosZ: Int
		get() = wrapped.serverPosZ

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
	// </editor-fold>

	// <editor-fold desc="Rotation">
	override var rotationYaw: Float
		get() = wrapped.rotationYaw
		set(value)
		{
			wrapped.rotationYaw = value
		}
	override var rotationPitch: Float
		get() = wrapped.rotationPitch
		set(value)
		{
			wrapped.rotationPitch = value
		}

	override var prevRotationYaw: Float
		get() = wrapped.prevRotationYaw
		set(value)
		{
			wrapped.prevRotationYaw = value
		}
	override var prevRotationPitch: Float
		get() = wrapped.prevRotationPitch
		set(value)
		{
			wrapped.prevRotationPitch = value
		}

	override val horizontalFacing: IEnumFacing
		get() = wrapped.horizontalFacing.wrap()
	override val lookVec: WVec3?
		get() = wrapped.lookVec.wrap()
	// </editor-fold>

	// <editor-fold desc="Border">
	override val width: Float
		get() = wrapped.width
	override val height: Float
		get() = wrapped.height
	override val eyeHeight: Float
		get() = wrapped.eyeHeight

	override val collisionBorderSize: Float
		get() = wrapped.collisionBorderSize
	override var entityBoundingBox: IAxisAlignedBB
		get() = wrapped.entityBoundingBox.wrap()
		set(value)
		{
			wrapped.entityBoundingBox = value.unwrap()
		}
	// </editor-fold>

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
	override var fallDistance: Float
		get() = wrapped.fallDistance
		set(value)
		{
			wrapped.fallDistance = value
		}
	override val hurtResistantTime: Int
		get() = wrapped.hurtResistantTime
	override val air: Int
		get() = wrapped.air

	override val ridingEntity: IEntity?
		get() = wrapped.ridingEntity?.wrap()

	// <editor-fold desc="Type casting">
	override fun asEntityPlayer(): IEntityPlayer = EntityPlayerImpl(wrapped as EntityPlayer)

	override fun asEntityLivingBase(): IEntityLivingBase = EntityLivingBaseImpl(wrapped as EntityLivingBase)

	override fun asEntityTNTPrimed(): IEntityTNTPrimed = EntityTNTPrimedImpl(wrapped as EntityTNTPrimed)

	override fun asEntityArrow(): IEntityArrow = EntityArrowImpl(wrapped as EntityArrow)

	override fun asEntityPotion(): IEntityPotion = EntityPotionImpl(wrapped as EntityPotion)

	override fun asEntityFishHook(): IEntityFishHook = EntityFishHookImpl(wrapped as EntityFishHook)

	override fun asEntityCreeper(): IEntityCreeper = EntityCreeperImpl(wrapped as EntityCreeper)
	// </editor-fold>

	// <editor-fold desc="Position & Movement">
	override fun getPositionEyes(partialTicks: Float): WVec3 = wrapped.getPositionEyes(partialTicks).wrap()
	override fun getLook(partialTicks: Float): WVec3 = wrapped.getLook(partialTicks).wrap()

	override fun moveEntity(x: Double, y: Double, z: Double) = wrapped.moveEntity(x, y, z)
	override fun setPosition(x: Double, y: Double, z: Double) = wrapped.setPosition(x, y, z)
	override fun setPositionAndUpdate(posX: Double, posY: Double, posZ: Double) = wrapped.setPositionAndUpdate(posX, posY, posZ)
	override fun setPositionAndRotation(posX: Double, posY: Double, posZ: Double, rotationYaw: Float, rotationPitch: Float) = wrapped.setPositionAndRotation(posX, posY, posZ, rotationYaw, rotationPitch)

	override fun copyLocationAndAnglesFrom(player: IEntity) = wrapped.copyLocationAndAnglesFrom(player.unwrap())
	// </editor-fold>

	// <editor-fold desc="Distance">
	override fun getDistance(x: Double, y: Double, z: Double): Double = wrapped.getDistance(x, y, z)
	override fun getDistanceToEntity(it: IEntity): Float = wrapped.getDistanceToEntity(it.unwrap())

	override fun getDistanceSq(blockPos: WBlockPos): Double = wrapped.getDistanceSq(blockPos.unwrap())
	override fun getDistanceSq(x: Double, y: Double, z: Double): Double = wrapped.getDistanceSq(x, y, z)
	override fun getDistanceSqToEntity(it: IEntity): Double = wrapped.getDistanceSqToEntity(it.unwrap())
	// </editor-fold>

	override fun rayTrace(range: Double, partialTicks: Float): IMovingObjectPosition? = wrapped.rayTrace(range, partialTicks).wrap()

	override fun isInsideOfMaterial(material: IMaterial): Boolean = wrapped.isInsideOfMaterial(material.unwrap())

	override fun canBeCollidedWith(): Boolean = wrapped.canBeCollidedWith()

	override fun setCanBeCollidedWith(value: Boolean) = (wrapped as IMixinEntityLivingBase).setCanBeCollidedWith(value)

	override fun canRiderInteract(): Boolean = wrapped.canRiderInteract()

	override fun equals(other: Any?): Boolean = other is EntityImpl<*> && wrapped.isEntityEqual(other.unwrap())
}

fun IEntity.unwrap(): Entity = (this as EntityImpl<*>).wrapped
fun Entity.wrap(): IEntity = EntityImpl(this)
