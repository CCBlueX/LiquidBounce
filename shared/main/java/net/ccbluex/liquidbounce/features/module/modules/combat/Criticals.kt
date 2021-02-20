/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import kotlin.random.Random

@ModuleInfo(name = "Criticals", description = "Automatically deals critical hits.", category = ModuleCategory.COMBAT)
class Criticals : Module()
{
	/**
	 * Options
	 */
	val modeValue = ListValue("Mode", arrayOf("Packet", "NCPPacket", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "Custom", "Visual"), "Packet")

	private val maxDelayValue = IntegerValue("MaxDelay", 0, 0, 500)
	private val minDelayValue = IntegerValue("MinDelay", 0, 0, 500)

	private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
	private val hitChanceValue = IntegerValue("Chance", 100, 0, 100)

	/**
	 * Custom Criticals Options
	 */
	private val customStepsValue = IntegerValue("Custom-Steps", 3, 2, 6)

	private val customYStep1Value = FloatValue("Custom-Step1", 0.11F, 0f, 2.9f)
	private val customYStep2Value = FloatValue("Custom-Step2", 0.1100013579F, 0f, 2.9f)
	private val customYStep3Value = FloatValue("Custom-Step3", 0.0000013579F, 0f, 2.9f)
	private val customYStep4Value = FloatValue("Custom-Step4", 0f, 0f, 2.9f)
	private val customYStep5Value = FloatValue("Custom-Step5", 0f, 0f, 2.9f)
	private val customYStep6Value = FloatValue("Custom-Step6", 0f, 0f, 2.9f)

	/**
	 * Delay Timer
	 */
	private val delayTimer = MSTimer()
	private var nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	override fun onEnable()
	{
		if (modeValue.get().equals("NoGround", ignoreCase = true)) (mc.thePlayer ?: return).jump()
	}

	@EventTarget
	fun onAttack(event: AttackEvent)
	{
		val provider = classProvider

		if (provider.isEntityLivingBase(event.targetEntity))
		{
			val thePlayer = mc.thePlayer ?: return
			val entity = (event.targetEntity ?: return)
			if (!provider.isEntityLivingBase(entity)) return
			val targetEntity = entity.asEntityLivingBase()

			val networkManager = mc.netHandler.networkManager

			val chance = hitChanceValue.get()
			if (!thePlayer.onGround || thePlayer.isOnLadder || thePlayer.ridingEntity != null || targetEntity.hurtTime > hurtTimeValue.get() || !(chance > 0 && Random.nextInt(100) <= chance) || LiquidBounce.moduleManager[Fly::class.java].state || !canCritical(thePlayer)) return

			val x = thePlayer.posX
			val y = thePlayer.posY
			val z = thePlayer.posZ

			when (modeValue.get().toLowerCase())
			{
				"packet" ->
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.0625, z, true))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.000011, z, false))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y, z, false))
					thePlayer.onCriticalHit(targetEntity)
				}

				"ncppacket" ->
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.11, z, false))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.1100013579, z, false))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.0000013579, z, false))
					thePlayer.onCriticalHit(targetEntity)
				}

				"tphop" ->
				{
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.02, z, false))
					networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + 0.01, z, false))
					thePlayer.setPosition(x, y + 0.01, z)
					thePlayer.onCriticalHit(targetEntity)
				}

				"custom" ->
				{
					val ystep = arrayOf(customYStep1Value.get(), customYStep2Value.get(), customYStep3Value.get(), customYStep4Value.get(), customYStep5Value.get(), customYStep6Value.get())

					for (i in 0 until customStepsValue.get()) networkManager.sendPacketWithoutEvent(provider.createCPacketPlayerPosition(x, y + ystep[i], z, false))

					thePlayer.onCriticalHit(targetEntity)
				}

				"hop" ->
				{
					thePlayer.motionY = 0.1
					thePlayer.fallDistance = 0.1f
					thePlayer.onGround = false
				}

				"jump" -> thePlayer.motionY = 0.42
				"lowjump" -> thePlayer.motionY = 0.3425
				"visual" -> thePlayer.onCriticalHit(targetEntity)
			}

			delayTimer.reset()
			nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
		}
	}

	fun canCritical(thePlayer: IEntityPlayerSP): Boolean = !thePlayer.isInWeb && !thePlayer.isInWater && !thePlayer.isInLava && delayTimer.hasTimePassed(nextDelay)

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketPlayer(packet) && modeValue.get().equals("NoGround", ignoreCase = true)) packet.asCPacketPlayer().onGround = false
	}

	override val tag: String
		get() = modeValue.get()
}
