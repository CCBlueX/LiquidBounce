/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isClientTarget
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.opengl.GL11.*
import kotlin.math.*

// I'm not an author of this TargetStrafe code. Original author: CzechHek (Converted from JS https://github.com/CzechHek/Core/blob/master/Scripts/TargetStrafe.js)
// I'm not an author of this 'Circle' code. Original author: Auto-reply bot (https://forums.ccbluex.net/topic/1574/script-circle/2?_=1623645819000)

// TODO: Adaptive path
@ModuleInfo(name = "TargetStrafe", description = "", category = ModuleCategory.MOVEMENT)
class TargetStrafe : Module()
{
	/**
	 * Strafe target
	 */
	private val targetModeValue = ListValue("TargetMode", arrayOf("KillAuraTarget", "Distance", "Health", "LivingTime"), "Distance")
	private val detectRangeValue = FloatValue("TargetRange", 6F, 1F, 16.0F)

	/**
	 * Strafe range
	 */
	private val strafeStartRangeValue = FloatValue("StrafeStartRange", 0F, 0F, 3F)
	private val strafeRangeValue = FloatValue("StrafeRange", 3F, 0.5F, 8.0F)

	/**
	 * Strafe target FoV
	 */
	private val fovValue = FloatValue("FoV", 180F, 30F, 180F)

	/**
	 * Should automatically jump while strafing?
	 */
	private val autoJumpValue = BoolValue("AutoJump", true)

	/**
	 * Strafe when player is on ground (bypass Strafe checks)
	 */
	private val onlyGroundValue = BoolValue("OnlyGround", false)

	/**
	 * Draw the strafe path
	 */
	private val drawPathValue = BoolValue("DrawPath", true)
	private val pathRenderAccuracyValue = FloatValue("DrawPathAccuracy", 5F, 0.5F, 20F)

	/**
	 * Strafe target priority
	 */
	private val priorityValue = ListValue("Priority", arrayOf("Encirclement", "Strafe"), "Encirclement")

	/**
	 * The strafe target
	 */
	private var target: IEntityLivingBase? = null

	/**
	 * Is targetstrafe active?
	 */
	private var strafing = false

	/**
	 * Strafe direction
	 */
	private var direction = -1F
	private var lastStrafeDirection = 0F

	// TODO: Adaptive path

	@EventTarget
	fun onMove(event: MoveEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		strafing = false

		val target = target ?: return

		val func = functions

		// Change direction
		if (thePlayer.moveStrafing != 0F && sign(thePlayer.moveStrafing) != lastStrafeDirection)
		{
			direction *= -1F
			lastStrafeDirection = sign(thePlayer.moveStrafing)
		}

		if (thePlayer.moveForward > 0F && !mc.gameSettings.keyBindSneak.pressed && (thePlayer.onGround || !onlyGroundValue.get()))
		{
			val strafeRange = strafeRangeValue.get()

			// Movement speed
			val moveSpeed = hypot(event.x, event.z)

			// Positions of the player
			val playerPosX = thePlayer.posX
			val playerPosZ = thePlayer.posZ

			// Positions of the strafe target
			val targetPosX = target.posX
			val targetPosY = target.posY
			val targetPosZ = target.posZ

			// Distance between the player and the strafe target
			val xDelta = targetPosX - playerPosX
			val zDelta = targetPosZ - playerPosZ
			val distance = hypot(xDelta, zDelta)

			// If the
			if (distance - moveSpeed > strafeRange + strafeStartRangeValue.get()) return

			// Strafe yaw radians
			val strafeYawRadians = atan2(zDelta, xDelta).toFloat()

			// Encirclement yaw radians
			val encirclementYawRadians = strafeYawRadians - WMathHelper.PI * 0.5F

			// FoV check
			if (abs(RotationUtils.getAngleDifference(WMathHelper.toDegrees(encirclementYawRadians), thePlayer.rotationYaw)) > fovValue.get()) return

			// Predict next position of the target and check it is safe
			val predict = targetPosX + (targetPosX - target.lastTickPosX) * 2.0 to targetPosZ + (targetPosZ - target.lastTickPosZ) * 2.0
			if (!isAboveGround(theWorld, predict.first, targetPosY, predict.second)) return

			// Setup encirclement movements
			val encirclementSpeed = distance - strafeRange
			val encirclementSpeedLimited = sign(encirclementSpeed) * min(abs(encirclementSpeed), moveSpeed)
			val encirclementX = -func.sin(encirclementYawRadians) * encirclementSpeedLimited
			val encirclementZ = func.cos(encirclementYawRadians) * encirclementSpeedLimited

			// Setup strafe movements
			val strafeSpeed = moveSpeed - if (priorityValue.get().equals("Encirclement", ignoreCase = true)) hypot(encirclementX, encirclementZ) else 0.0
			var strafeX = -func.sin(strafeYawRadians) * strafeSpeed * direction
			var strafeZ = func.cos(strafeYawRadians) * strafeSpeed * direction

			if (thePlayer.onGround && (thePlayer.isCollidedHorizontally || !isAboveGround(theWorld, playerPosX + encirclementX + strafeX * 2, thePlayer.posY, playerPosZ + encirclementZ + strafeZ * 2)))
			{
				direction *= -1F
				strafeX *= -1
				strafeZ *= -1
			}

			// TODO: Better calculation algorithm (current one is the ugliest one)
			val resultYawRadians = WMathHelper.toRadians(WMathHelper.wrapAngleTo180_float(WMathHelper.toDegrees(StrictMath.atan2(encirclementZ + strafeZ, encirclementX + strafeX).toFloat()) - 90.0f))
			event.x = -func.sin(resultYawRadians) * moveSpeed
			event.z = func.cos(resultYawRadians) * moveSpeed

			strafing = true
		}
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		if (strafing && thePlayer.onGround && autoJumpValue.get()) thePlayer.jump()
	}

	@EventTarget
	fun onMotion(event: MotionEvent)
	{
		if (event.eventState == EventState.POST)
		{
			val theWorld = mc.theWorld ?: return
			val thePlayer = mc.thePlayer ?: return

			val targetRange = detectRangeValue.get()
			val checkIsClientTarget = { entity: IEntity -> if (entity.isClientTarget()) -1000000.0 else 0.0 }

			target = if (targetModeValue.get().equals("KillAuraTarget", ignoreCase = true)) (LiquidBounce.moduleManager[KillAura::class.java] as KillAura).target
			else EntityUtils.getEntitiesInRadius(theWorld, thePlayer).filter { EntityUtils.isSelected(it, true) }.map(IEntity::asEntityLivingBase).filter { thePlayer.getDistanceToEntityBox(it) <= targetRange }.minBy {
				when (targetModeValue.get().toLowerCase())
				{
					"livingtime" -> -it.ticksExisted.toFloat()
					"health" -> it.health
					else -> thePlayer.getDistanceToEntityBox(it).toFloat()
				} + checkIsClientTarget(it)
			}
		}
	}

	@EventTarget
	fun onRender3D(event: Render3DEvent)
	{
		if (!drawPathValue.get()) return

		val target = target ?: return

		val partialTicks = event.partialTicks
		val renderManager = mc.renderManager

		glPushMatrix()
		glTranslated(target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks - renderManager.renderPosX, target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks - renderManager.renderPosY, target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks - renderManager.renderPosZ)
		RenderUtils.drawRadius(strafeRangeValue.get(), pathRenderAccuracyValue.get(), if (strafing) -40864 else -1)
		glPopMatrix()
	}

	private fun isAboveGround(theWorld: IWorldClient, x: Double, y: Double, z: Double): Boolean
	{
		var i = ceil(y)
		while ((y - 5) < i--) if (!classProvider.isBlockAir(BlockUtils.getBlock(theWorld, WBlockPos(x, i, z)))) return true

		return false
	}

	override val tag: String
		get() = targetModeValue.get()
}
