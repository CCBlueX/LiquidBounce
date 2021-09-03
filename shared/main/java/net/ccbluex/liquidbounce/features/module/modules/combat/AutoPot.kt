/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventState.POST
import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import kotlin.random.Random

@ModuleInfo(name = "AutoPot", description = "Automatically throws healing potions.", category = ModuleCategory.COMBAT)
class AutoPot : Module()
{
	private val modeValue = ListValue("Mode", arrayOf("Normal", "Jump", "Port"), "Normal")
	private val throwDirValue = ListValue("ThrowDirection", arrayOf("Up", "Down"), "Down")
	private val healthValue = FloatValue("Health", 15F, 1F, 20F)
	private val potDelayValue = IntegerRangeValue("Delay", 250, 250, 0, 2000, "MaxPotDelay" to "MinPotDelay")
	private val silentValue = BoolValue("Silent", true)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)
	private val noMoveThrowValue = BoolValue("NoMove", false, "NoMove-Throw")
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val groundDistanceValue = FloatValue("GroundDistance", 2F, 0.72F, 5F)
	private val ignoreScreen = BoolValue("IgnoreScreen", true)

	private val inventoryGroup = ValueGroup("Inventory")
	private val inventoryDelayValue = IntegerRangeValue("Delay", 100, 200, 0, 5000, "MaxInvDelay" to "MinInvDelay")
	private val inventoryOpenInventoryValue = BoolValue("OpenInventory", false, "OpenInv")
	private val inventorySimulateInventoryValue = BoolValue("SimulateInventory", true, "SimulateInventory")
	private val inventoryNoMoveValue = BoolValue("NoMove", false, "NoMove")
	private val inventoryRandomSlotValue = BoolValue("RandomSlot", false)

	private val inventoryMisclickGroup = ValueGroup("ClickMistakes")
	private val inventoryMisclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
	private val inventoryMisclickRateValue = IntegerValue("Rate", 5, 0, 100, "ClickMistakeRate")

	private val rotationGroup = ValueGroup("Rotation")
	private val rotationEnabledValue = BoolValue("Enabled", true, "Rotations")
	private val rotationAccelerationRatioValue = FloatRangeValue("Acceleration", 0f, 0f, 0f, .99f, "MaxAccelerationRatio" to "MinAccelerationRatio")
	private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 1f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
	private val rotationResetSpeedValue = FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")

	private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
	private val rotationKeepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
	private val rotationKeepRotationTicksValue = IntegerValue("Ticks", 1, 1, 40, "KeepRotationLength")

	private val killAuraBypassGroup = ValueGroup("KillAuraBypass")
	private val killauraBypassModeValue = ListValue("Mode", arrayOf("None", "SuspendKillAura", "WaitForKillauraEnd"), "SuspendKillAura", "KillauraBypassMode")
	private val killAuraBypassKillAuraSuspendDurationValue = object : IntegerValue("Duration", 300, 100, 1000, "SuspendKillauraDuration")
	{
		override fun showCondition() = killauraBypassModeValue.get().equals("SuspendKillAura", ignoreCase = true)
	}

	private val potionFilterGroup = ValueGroup("PotionFilter") // TODO: Add potions
	private val potionFilterInvisibleValue = BoolValue("InvisibilityPot", false, "InvisibilityPot")

	private val potionFilterJumpBoostGroup = ValueGroup("JumpBoost")
	private val potionFilterJumpBoostEnabledValue = BoolValue("Enabled", true, "JumpBoostPot")
	private val potionFilterJumpBoostAmpLimitValue = IntegerValue("AmplifierLimit", 5, 2, 127, "JumpPotAmplifierLimit")

	private val potThrowDelayTimer = MSTimer()
	private var potThrowDelay = potDelayValue.getRandomDelay()
	private var invDelay = inventoryDelayValue.getRandomDelay()

	private var potion = -1

	init
	{
		inventoryMisclickGroup.addAll(inventoryMisclickEnabledValue, inventoryMisclickRateValue)
		inventoryGroup.addAll(inventoryDelayValue, inventoryOpenInventoryValue, inventorySimulateInventoryValue, inventoryNoMoveValue, inventoryRandomSlotValue, inventoryMisclickGroup)

		rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationTicksValue)
		rotationGroup.addAll(rotationEnabledValue, rotationAccelerationRatioValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationKeepRotationGroup)

		killAuraBypassGroup.addAll(killauraBypassModeValue, killAuraBypassKillAuraSuspendDurationValue)

		potionFilterJumpBoostGroup.addAll(potionFilterJumpBoostEnabledValue, potionFilterJumpBoostAmpLimitValue)
		potionFilterGroup.addAll(potionFilterInvisibleValue, potionFilterJumpBoostGroup)
	}

	companion object
	{
		@JvmField
		val goodEffects = arrayListOf<Int>()

		init
		{
			val provider = classProvider

			goodEffects.add(provider.getPotionEnum(PotionType.ABSORPTION).id)
			goodEffects.add(provider.getPotionEnum(PotionType.DAMAGE_BOOST).id)
			goodEffects.add(provider.getPotionEnum(PotionType.DIG_SPEED).id)
			goodEffects.add(provider.getPotionEnum(PotionType.HEAL).id)
			goodEffects.add(provider.getPotionEnum(PotionType.HEALTH_BOOST).id)
			goodEffects.add(provider.getPotionEnum(PotionType.INVISIBILITY).id)
			goodEffects.add(provider.getPotionEnum(PotionType.JUMP).id)
			goodEffects.add(provider.getPotionEnum(PotionType.MOVE_SPEED).id)
			goodEffects.add(provider.getPotionEnum(PotionType.NIGHT_VISION).id)
			goodEffects.add(provider.getPotionEnum(PotionType.REGENERATION).id)
			goodEffects.add(provider.getPotionEnum(PotionType.RESISTANCE).id)
			goodEffects.add(provider.getPotionEnum(PotionType.WATER_BREATHING).id)
			goodEffects.add(provider.getPotionEnum(PotionType.FIRE_RESISTANCE).id)
		}

		@JvmStatic
		fun isPotionUseful(item: IItemStack): Boolean
		{
			val potionItem = item.item ?: return false

			if (!classProvider.isItemPotion(potionItem)) return false

			return potionItem.asItemPotion().getEffects(item).any { goodEffects.contains(it.potionID) }
		}
	}

	@EventTarget
	fun onMotion(motionEvent: MotionEvent)
	{
		val controller = mc.playerController

		if (controller.isInCreativeMode) return

		val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
		if (killauraBypassModeValue.get().equals("WaitForKillauraEnd", true) && killAura.state && killAura.target != null) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val screen = mc.currentScreen
		val netHandler = mc.netHandler

		val inventoryContainer = thePlayer.inventoryContainer
		val activePotionEffects = thePlayer.activePotionEffects

		val randomSlot = randomSlotValue.get()
		val invRandomSlot = inventoryRandomSlotValue.get()
		val throwDirection = throwDirValue.get().toLowerCase()
		val health = healthValue.get()

		val provider = classProvider

		val containerOpen = provider.isGuiContainer(screen)
		val isNotInventory = !provider.isGuiInventory(screen)

		val serverRotation = RotationUtils.serverRotation
		val ignoreScreen = ignoreScreen.get()

		when (motionEvent.eventState)
		{
			PRE ->
			{
				if (potThrowDelayTimer.hasTimePassed(potThrowDelay) && (ignoreScreen || containerOpen) && !(noMoveThrowValue.get() && MovementUtils.isMoving(thePlayer)))
				{
					// Hotbar Potion
					val healPotionInHotbar = findHealPotion(thePlayer, 36, 45, inventoryContainer, randomSlot)
					val buffPotionInHotbar = findBuffPotion(activePotionEffects, 36, 45, inventoryContainer, randomSlot)

					if (thePlayer.health <= health && healPotionInHotbar != -1 || buffPotionInHotbar != -1)
					{
						if (thePlayer.onGround)
						{
							when (modeValue.get().toLowerCase())
							{
								"jump" -> thePlayer.jump()
								"port" -> thePlayer.moveEntity(0.0, 0.42, 0.0)
							}
						}

						val posY = thePlayer.posY

						// Prevent throwing potions into the void
						val fallingPlayer = FallingPlayer(theWorld, thePlayer, thePlayer.posX + thePlayer.motionX * 2, posY, thePlayer.posZ + thePlayer.motionZ * 2, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)

						val collisionBlock = fallingPlayer.findCollision(20)?.pos

						if (posY - (collisionBlock?.y ?: 0) >= groundDistanceValue.get()) return

						// Suspend killaura if option is present
						if (killauraBypassModeValue.get().equals("SuspendKillaura", true)) killAura.suspend(killAuraBypassKillAuraSuspendDurationValue.get().toLong())

						potion = if (thePlayer.health <= health && healPotionInHotbar != -1) healPotionInHotbar else buffPotionInHotbar

						val potionIndex = potion - 36

						if (silentValue.get())
						{
							if (InventoryUtils.setHeldItemSlot(thePlayer, potionIndex, -1, true)) return
						}
						else
						{
							thePlayer.inventory.currentItem = potionIndex
							controller.updateController()
						}

						// // Swap hotbar slot to potion slot
						// netHandler.addToSendQueue(provider.createCPacketHeldItemChange(potion - 36))

						val pitch = thePlayer.rotationPitch

						if (!rotationEnabledValue.get() || rotationTurnSpeedValue.getMax() <= 0F) return

						if (if (throwDirection == "up") pitch > -80F else pitch < 80F)
						{
							// Limit TurnSpeed
							val turnSpeed = rotationTurnSpeedValue.getRandomStrict()

							// Acceleration
							val acceleration = rotationAccelerationRatioValue.getRandomStrict()

							val targetRotation = Rotation(thePlayer.rotationYaw, RandomUtils.nextFloat(if (throwDirection == "up") -80F else 80F, if (throwDirection == "up") -90F else 90F))

							RotationUtils.setTargetRotation(RotationUtils.limitAngleChange(serverRotation, targetRotation, turnSpeed, acceleration), if (rotationKeepRotationEnabledValue.get()) rotationKeepRotationTicksValue.get() else 0)

							val maxResetSpeed = rotationResetSpeedValue.getMax().coerceAtLeast(10F)
							val minResetSpeed = rotationResetSpeedValue.getMin().coerceAtLeast(10F)
							if (maxResetSpeed < 180) RotationUtils.setNextResetTurnSpeed(minResetSpeed, maxResetSpeed)
						}

						return
					}
				}

				val currentContainer = thePlayer.openContainer
				if (InventoryUtils.CLICK_TIMER.hasTimePassed(invDelay) && !(inventoryNoMoveValue.get() && MovementUtils.isMoving(thePlayer)) && !(currentContainer != null && currentContainer.windowId != 0))
				{
					// Move Potion Inventory -> Hotbar
					val healPotionInInventory = findHealPotion(thePlayer, 9, 36, inventoryContainer, invRandomSlot)
					val buffPotionInInventory = findBuffPotion(activePotionEffects, 9, 36, inventoryContainer, invRandomSlot)

					if ((healPotionInInventory != -1 || buffPotionInInventory != -1) && InventoryUtils.hasSpaceHotbar(thePlayer.inventory))
					{
						if (inventoryOpenInventoryValue.get() && isNotInventory) return

						var slot = if (healPotionInInventory != -1) healPotionInInventory else buffPotionInInventory

						val misclickRate = inventoryMisclickRateValue.get()

						// Simulate Click Mistakes to bypass some (geek) anti-cheat's click accuracy checks
						if (inventoryMisclickEnabledValue.get() && misclickRate > 0 && Random.nextInt(100) <= misclickRate)
						{
							val firstEmpty = InventoryUtils.firstEmpty(thePlayer.inventoryContainer, 9, 36, invRandomSlot)
							if (firstEmpty != -1) slot = firstEmpty
						}

						val openInventory = isNotInventory && inventorySimulateInventoryValue.get()

						if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

						controller.windowClick(0, slot, 0, 1, thePlayer)

						if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

						invDelay = inventoryDelayValue.getRandomDelay()
						InventoryUtils.CLICK_TIMER.reset()
					}
				}
			}

			POST ->
			{
				val serverPitch = serverRotation.pitch
				val pitchCheck = if (throwDirection == "up") serverPitch <= -75F else serverPitch >= 75F

				if ((ignoreScreen || !containerOpen) && potion >= 0 && pitchCheck)
				{
					val itemStack = thePlayer.inventoryContainer.getSlot(potion).stack

					if (itemStack != null)
					{
						netHandler.addToSendQueue(createUseItemPacket(itemStack, WEnumHand.MAIN_HAND))

						if (silentValue.get()) InventoryUtils.reset(thePlayer)

						potThrowDelay = potDelayValue.getRandomDelay()
						potThrowDelayTimer.reset()
					}

					potion = -1
				}
			}
		}
	}

	fun findHealPotion(thePlayer: IEntityLivingBase, startSlot: Int, endSlot: Int, inventoryContainer: IContainer, random: Boolean, splash: Boolean = true): Int
	{
		val provider = classProvider

		val candidates = mutableListOf<Int>()

		val regenPotion = provider.getPotionEnum(PotionType.REGENERATION)

		val healID = provider.getPotionEnum(PotionType.HEAL).id
		val regenID = regenPotion.id

		val playerRegen = thePlayer.isPotionActive(regenPotion)

		(startSlot until endSlot).mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { provider.isItemPotion(it.second.item) }.run { if (splash) filter { it.second.isSplash() } else filterNot { it.second.isSplash() } }.forEach { (slotIndex, stack) ->
			var heal = false
			var regen = false

			(stack.item ?: return@forEach).asItemPotion().getEffects(stack).map(IPotionEffect::potionID).forEach { potionID ->
				if (potionID == healID) heal = true
				if (potionID == regenID) regen = true
			}

			if (!candidates.contains(slotIndex))
			{
				if (heal) candidates.add(slotIndex)
				if (!playerRegen && regen) candidates.add(slotIndex)
			}
		}

		return when
		{
			candidates.isEmpty() -> -1
			random -> candidates.random()
			else -> candidates.first()
		}
	}

	fun findBuffPotion(activePotionEffects: Collection<IPotionEffect>, startSlot: Int, endSlot: Int, inventoryContainer: IContainer, random: Boolean, splash: Boolean = true, itemDelay: Long = itemDelayValue.get().toLong()): Int
	{
		val provider = classProvider

		val jumpPot = potionFilterJumpBoostEnabledValue.get()
		val invisPot = potionFilterInvisibleValue.get()
		val jumpPotionAmplifierLimit = potionFilterJumpBoostAmpLimitValue.get()

		var playerSpeed = -1
		var playerJump = -1
		var playerDigSpeed = -1
		var playerDamageBoost = -1
		var playerResis = -1
		var playerAbsorp = -1
		var playerHealthBoost = -1
		var playerWaterBreath = -1

		var playerFireResis = false
		var playerInvis = false
		var playerNightVision = false

		val speedID = provider.getPotionEnum(PotionType.MOVE_SPEED).id
		val jumpID = provider.getPotionEnum(PotionType.JUMP).id
		val hasteID = provider.getPotionEnum(PotionType.DIG_SPEED).id
		val strengthID = provider.getPotionEnum(PotionType.DAMAGE_BOOST).id
		val fireResisID = provider.getPotionEnum(PotionType.FIRE_RESISTANCE).id
		val resisID = provider.getPotionEnum(PotionType.RESISTANCE).id
		val absorptionID = provider.getPotionEnum(PotionType.ABSORPTION).id
		val healthBoostID = provider.getPotionEnum(PotionType.HEALTH_BOOST).id
		val waterBreathID = provider.getPotionEnum(PotionType.WATER_BREATHING).id
		val invisID = provider.getPotionEnum(PotionType.INVISIBILITY).id
		val nightVisionID = provider.getPotionEnum(PotionType.NIGHT_VISION).id

		activePotionEffects.map { it.potionID to it.amplifier }.forEach { (potionID, amplifier) ->
			if (potionID == speedID) playerSpeed = amplifier
			if (jumpPot && potionID == jumpID) playerJump = amplifier
			if (potionID == hasteID) playerDigSpeed = amplifier
			if (potionID == strengthID) playerDamageBoost = amplifier
			if (potionID == resisID) playerResis = amplifier
			if (potionID == absorptionID) playerAbsorp = amplifier
			if (potionID == healthBoostID) playerHealthBoost = amplifier
			if (potionID == waterBreathID) playerWaterBreath = amplifier

			if (potionID == fireResisID) playerFireResis = true
			if (invisPot && potionID == invisID) playerInvis = true
			if (potionID == nightVisionID) playerNightVision = true
		}

		val candidates = mutableListOf<Int>()

		val currentTime = System.currentTimeMillis()

		(startSlot until endSlot).asSequence().mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { currentTime - it.second.itemDelay >= itemDelay }.filter { provider.isItemPotion(it.second.item) }.run { if (splash) filter { it.second.isSplash() } else filterNot { it.second.isSplash() } }.forEach { (slotIndex, stack) ->
			var potionSpeed = -1
			var potionJump = -1
			var potionDigSpeed = -1
			var potionDamageBoost = -1
			var potionResis = -1
			var potionAbsorp = -1
			var potionHealthboost = -1
			var potionWaterBreath = -1

			var potionFireResis = false
			var potionInvis = false
			var potionNightVision = false

			(stack.item ?: return@forEach).asItemPotion().getEffects(stack).map { it.potionID to it.amplifier }.forEach { (potionID, amplifier) ->
				if (potionID == speedID) potionSpeed = amplifier
				if (jumpPot && potionID == jumpID && amplifier <= jumpPotionAmplifierLimit) potionJump = amplifier
				if (potionID == hasteID) potionDigSpeed = amplifier
				if (potionID == strengthID) potionDamageBoost = amplifier
				if (potionID == resisID) potionResis = amplifier
				if (potionID == absorptionID) potionAbsorp = amplifier
				if (potionID == healthBoostID) potionHealthboost = amplifier
				if (potionID == waterBreathID) potionWaterBreath = amplifier

				if (potionID == fireResisID) potionFireResis = true
				if (invisPot && potionID == invisID) potionInvis = true
				if (potionID == nightVisionID) potionNightVision = true
			}
			//</editor-fold>

			if (!candidates.contains(slotIndex))
			{
				// Speed Splash Potion
				if (potionSpeed > -1 && playerSpeed < potionSpeed) candidates.add(slotIndex)

				// Strength(damage boost) Splash Potion
				if (potionDamageBoost > -1 && potionDamageBoost > playerDamageBoost) candidates.add(slotIndex)

				// Resistance Splash Potion
				if (potionResis > -1 && potionResis > playerResis) candidates.add(slotIndex)

				// DigSpeed Potion
				if (potionDigSpeed > -1 && potionDigSpeed > playerDigSpeed) candidates.add(slotIndex)

				// Absorption Splash Potion
				if (potionAbsorp > -1 && potionAbsorp > playerAbsorp) candidates.add(slotIndex)

				// Health Boost Splash Potion
				if (potionHealthboost > -1 && potionHealthboost > playerHealthBoost) candidates.add(slotIndex)

				// Night Vision Splash Potion
				if (potionNightVision && !playerNightVision) candidates.add(slotIndex)

				// Water Breathing Splash Potion
				if (potionWaterBreath > -1 && potionWaterBreath > playerWaterBreath) candidates.add(slotIndex)

				// Fire Resistance Splash Potion
				if (potionFireResis && !playerFireResis) candidates.add(slotIndex)

				// Jump Boost Splash Potion
				if (jumpPot && potionJump > -1 && potionJump > playerJump) candidates.add(slotIndex)

				// Invisibility Splash Potion
				if (invisPot && potionInvis && !playerInvis) candidates.add(slotIndex)
			}
		}

		return when
		{
			candidates.isEmpty() -> -1
			random -> candidates.random()
			else -> candidates.first()
		}
	}

	override val tag: String
		get() = "${healthValue.get()}"
}
