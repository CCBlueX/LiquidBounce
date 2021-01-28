/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.PathUtils.findPath
import net.ccbluex.liquidbounce.utils.RaycastUtils.EntityFilter
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import javax.vecmath.Vector3d

@ModuleInfo(name = "TeleportHit", description = "Allows to hit entities from far away.", category = ModuleCategory.COMBAT)
class TeleportHit : Module()
{
	private var targetEntity: IEntityLivingBase? = null
	private var shouldHit = false

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		if (event.eventState !== EventState.PRE) return
		val thePlayer = mc.thePlayer ?: return

		val facedEntity = raycastEntity(100.0, object : EntityFilter
		{
			override fun canRaycast(entity: IEntity?): Boolean = classProvider.isEntityLivingBase(entity)
		})
		if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true) && facedEntity!!.getDistanceSqToEntity(thePlayer) >= 1.0) targetEntity = facedEntity.asEntityLivingBase()

		if (targetEntity != null)
		{
			val currentTarget = targetEntity!!

			if (!shouldHit)
			{
				shouldHit = true
				return
			}

			if (thePlayer.fallDistance > 0.0f)
			{
				val rotationVector = RotationUtils.getVectorForRotation(Rotation(thePlayer.rotationYaw, 0.0f))

				val x = thePlayer.posX + rotationVector.xCoord * (thePlayer.getDistanceToEntity(currentTarget) - 1.0f)
				val y = currentTarget.position.y + 0.25
				val z = thePlayer.posZ + rotationVector.zCoord * (thePlayer.getDistanceToEntity(currentTarget) - 1.0f)

				findPath(thePlayer, x, y + 1.0, z, 4.0).forEach { pos: Vector3d -> mc.netHandler.addToSendQueue(classProvider.createCPacketPlayerPosition(pos.getX(), pos.getY(), pos.getZ(), false)) }

				thePlayer.swingItem()
				mc.netHandler.addToSendQueue(classProvider.createCPacketUseEntity(currentTarget, ICPacketUseEntity.WAction.ATTACK))
				thePlayer.onCriticalHit(currentTarget)

				shouldHit = false
				targetEntity = null
			} else if (thePlayer.onGround) thePlayer.jump()
		} else shouldHit = false
	}
}
