/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
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
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.*
import kotlin.random.Random

@ModuleInfo(name = "AutoPot", description = "Automatically throws healing potions.", category = ModuleCategory.COMBAT)
class AutoPot : Module()
{

	private val modeValue = ListValue("Mode", arrayOf("Normal", "Jump", "Port"), "Normal")
	private val throwDirValue = ListValue("ThrowDirection", arrayOf("Up", "Down"), "Down")

	private val healthValue = FloatValue("Health", 15F, 1F, 20F)

	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxPotDelay", 250, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minDelayValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minDelayValue: IntegerValue = object : IntegerValue("MinPotDelay", 250, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxDelayValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val maxInvDelayValue: IntegerValue = object : IntegerValue("MaxInvDelay", 200, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minInvDelayValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minInvDelayValue: IntegerValue = object : IntegerValue("MinInvDelay", 100, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxInvDelayValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val openInventoryValue = BoolValue("OpenInv", false)
	private val simulateInventory = BoolValue("SimulateInventory", true)
	private val noMoveValue = BoolValue("NoMove", false)
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val misClickValue = BoolValue("ClickMistakes", false)
	private val misClickRateValue = IntegerValue("ClickMistakeRate", 5, 0, 100)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	private val groundDistanceValue = FloatValue("GroundDistance", 2F, 0F, 5F)

	private val ignoreScreen = BoolValue("IgnoreScreen", true)

	private val keepRotationValue = BoolValue("KeepRotation", false)
	private val keepRotationLengthValue = IntegerValue("KeepRotationLength", 1, 1, 40)

	private val killauraBypassValue = ListValue("KillauraBypassMode", arrayOf("None", "SuspendKillaura", "WaitForKillauraEnd"), "SuspendKillaura")
	private val suspendKillauraDuration: IntegerValue = IntegerValue("SuspendKillauraDuration", 250, 0, 1000)

	private val invisPotValue = BoolValue("InvisibilityPot", false)
	private val jumpBoostPotValue = BoolValue("JumpBoostPot", true)
	private val jumpPotAmpLimitValue = IntegerValue("JumpPotAmplifierLimit", 5, 2, 127)

	private val potThrowDelayTimer = MSTimer()
	private var potThrowDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
	private val invDelayTimer = MSTimer()
	private var invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())

	private var potion = -1

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
			if (!classProvider.isItemPotion(item.item)) return false
			return item.item!!.asItemPotion().getEffects(item).any { goodEffects.contains(it.potionID) }
		}
	}

	@EventTarget
	fun onMotion(motionEvent: MotionEvent)
	{
		val controller = mc.playerController

		if (controller.isInCreativeMode) return

		val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
		if (killauraBypassValue.get().equals("WaitForKillauraEnd", true) && killAura.state && killAura.target != null) return

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val screen = mc.currentScreen
		val netHandler = mc.netHandler

		val inventoryContainer = thePlayer.inventoryContainer
		val activePotionEffects = thePlayer.activePotionEffects

		val randomSlot = randomSlotValue.get()
		val throwDirection = throwDirValue.get().toLowerCase()
		val health = healthValue.get()

		val provider = classProvider

		val containerOpen = provider.isGuiContainer(screen)
		val isNotInventory = !provider.isGuiInventory(screen)

		when (motionEvent.eventState)
		{
			PRE ->
			{
				if (potThrowDelayTimer.hasTimePassed(potThrowDelay) && (ignoreScreen.get() || containerOpen))
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
						val fallingPlayer = FallingPlayer(theWorld, thePlayer, thePlayer.posX, posY, thePlayer.posZ, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)

						val collisionBlock = fallingPlayer.findCollision(20)?.pos

						if (posY - (collisionBlock?.y ?: 0) >= groundDistanceValue.get()) return

						// Suspend killaura if option is present
						if (killauraBypassValue.get().equals("SuspendKillaura", true)) killAura.suspend(suspendKillauraDuration.get().toLong())

						potion = if (thePlayer.health <= health && healPotionInHotbar != -1) healPotionInHotbar else buffPotionInHotbar

						// Swap hotbar slot to potion slot
						netHandler.addToSendQueue(provider.createCPacketHeldItemChange(potion - 36))

						val pitch = thePlayer.rotationPitch

						if (pitch <= 80F) RotationUtils.setTargetRotation(Rotation(thePlayer.rotationYaw, RandomUtils.nextFloat(80F, 90F)))

						if (when (throwDirection)
							{
								"up" -> pitch > -80F
								else -> pitch < 80F
							}) RotationUtils.setTargetRotation(Rotation(thePlayer.rotationYaw, RandomUtils.nextFloat(when (throwDirection)
						{
							"up" -> -80F
							else -> 80F
						}, when (throwDirection)
						{
							"up" -> -90F
							else -> 90F
						})), if (keepRotationValue.get()) keepRotationLengthValue.get() else 0)

						return
					}
				}

				val currentContainer = thePlayer.openContainer
				if (invDelayTimer.hasTimePassed(invDelay) && !(noMoveValue.get() && MovementUtils.isMoving(thePlayer)) && !(currentContainer != null && currentContainer.windowId != 0))
				{
					// Move Potion Inventory -> Hotbar
					val healPotionInInventory = findHealPotion(thePlayer, 9, 36, inventoryContainer, randomSlot)
					val buffPotionInInventory = findBuffPotion(activePotionEffects, 9, 36, inventoryContainer, randomSlot)

					if ((healPotionInInventory != -1 || buffPotionInInventory != -1) && InventoryUtils.hasSpaceHotbar(thePlayer.inventory))
					{
						if (openInventoryValue.get() && isNotInventory) return

						var slot = if (healPotionInInventory != -1) healPotionInInventory else buffPotionInInventory

						val misclickRate = misClickRateValue.get()

						// Simulate Click Mistakes to bypass some (geek) anti-cheat's click accuracy checks
						if (misClickValue.get() && misclickRate > 0 && Random.nextInt(100) <= misclickRate)
						{
							val firstEmpty = InventoryUtils.firstEmpty(thePlayer.inventoryContainer, 9, 36, randomSlot)
							if (firstEmpty != -1) slot = firstEmpty
						}

						val openInventory = isNotInventory && simulateInventory.get()

						if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

						controller.windowClick(0, slot, 0, 1, thePlayer)

						if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

						invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())
						invDelayTimer.reset()
					}
				}
			}

			POST ->
			{
				val pitchCheck = when (throwDirection)
				{
					"up" -> RotationUtils.serverRotation.pitch <= -75F
					else -> RotationUtils.serverRotation.pitch >= 75F
				}

				if ((ignoreScreen.get() || !containerOpen) && potion >= 0 && pitchCheck)
				{
					val itemStack = thePlayer.inventoryContainer.getSlot(potion).stack

					if (itemStack != null)
					{
						netHandler.addToSendQueue(createUseItemPacket(itemStack, WEnumHand.MAIN_HAND))
						netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

						potThrowDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
						potThrowDelayTimer.reset()
					}

					potion = -1
				}
			}
		}
	}

	private fun findHealPotion(thePlayer: IEntityPlayerSP, startSlot: Int, endSlot: Int, inventoryContainer: IContainer, random: Boolean): Int
	{
		val provider = classProvider

		val candidates = mutableListOf<Int>()

		val healID = provider.getPotionEnum(PotionType.HEAL).id
		val regenID = provider.getPotionEnum(PotionType.REGENERATION).id

		(startSlot until endSlot).mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { provider.isItemPotion(it.second.item) }.filter { it.second.isSplash() }.forEach { (slotIndex, stack) ->
			val effects = (stack.item ?: return@forEach).asItemPotion().getEffects(stack)

			if (effects.any { it.potionID == healID }) candidates.add(slotIndex)
			if (!thePlayer.isPotionActive(provider.getPotionEnum(PotionType.REGENERATION)) && effects.any { it.potionID == regenID }) candidates.add(slotIndex)
		}

		return when
		{
			candidates.isEmpty() -> -1
			random -> candidates.random()
			else -> candidates.first()
		}
	}

	private fun findBuffPotion(activePotionEffects: Collection<IPotionEffect>, startSlot: Int, endSlot: Int, inventoryContainer: IContainer, random: Boolean): Int
	{
		val provider = classProvider

		val jumpPot = jumpBoostPotValue.get()
		val invisPot = invisPotValue.get()
		val itemDelay = itemDelayValue.get()
		val jumpPotionAmplifierLimit = jumpPotAmpLimitValue.get()

		var playerSpeed = -1
		var playerJump = -1
		var playerDigSpeed = -1
		var playerDamageBoost = -1
		var playerFireResis = -1
		var playerResis = -1
		var playerAbsorp = -1
		var playerHealthBoost = -1
		var playerWaterBreath = -1

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
			if (potionID == fireResisID) playerFireResis = amplifier
			if (potionID == resisID) playerResis = amplifier
			if (potionID == absorptionID) playerAbsorp = amplifier
			if (potionID == healthBoostID) playerHealthBoost = amplifier
			if (potionID == waterBreathID) playerWaterBreath = amplifier

			if (invisPot && potionID == invisID) playerInvis = true
			if (potionID == nightVisionID) playerNightVision = true
		}

		val candidates = mutableListOf<Int>()

		(startSlot until endSlot).asSequence().mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { System.currentTimeMillis() - it.second.itemDelay >= itemDelay }.filter { provider.isItemPotion(it.second.item) }.filter { it.second.isSplash() }.forEach { (i, stack) ->
			var potionSpeed = -1
			var potionJump = -1
			var potionDigSpeed = -1
			var potionDamageBoost = -1
			var potionFireResis = -1
			var potionResis = -1
			var potionAbsorp = -1
			var potionHealthboost = -1
			var potionWaterBreath = -1

			var potionInvis = false
			var potionNightVision = false

			(stack.item ?: return@forEach).asItemPotion().getEffects(stack).map { it.potionID to it.amplifier }.forEach { (potionID, amplifier) ->
				if (potionID == speedID) potionSpeed = amplifier
				if (jumpPot && potionID == jumpID && amplifier <= jumpPotionAmplifierLimit) potionJump = amplifier
				if (potionID == hasteID) potionDigSpeed = amplifier
				if (potionID == strengthID) potionDamageBoost = amplifier
				if (potionID == fireResisID) potionFireResis = amplifier
				if (potionID == resisID) potionResis = amplifier
				if (potionID == absorptionID) potionAbsorp = amplifier
				if (potionID == healthBoostID) potionHealthboost = amplifier
				if (potionID == waterBreathID) potionWaterBreath = amplifier

				if (invisPot && potionID == invisID) potionInvis = true
				if (potionID == nightVisionID) potionNightVision = true
			}
			//</editor-fold>

			if (!candidates.contains(i))
			{
				// Speed Splash Potion
				if (potionSpeed > -1 && playerSpeed < potionSpeed) candidates.add(i)

				// Strength(damage boost) Splash Potion
				if (potionDamageBoost > -1 && potionDamageBoost > playerDamageBoost) candidates.add(i)

				// Resistance Splash Potion
				if (potionResis > -1 && potionResis > playerResis) candidates.add(i)

				// Fire Resistance Splash Potion
				if (potionFireResis > -1 && potionFireResis > playerFireResis) candidates.add(i)

				// DigSpeed Potion
				if (potionDigSpeed > -1 && potionDigSpeed > playerDigSpeed) candidates.add(i)

				// Absorption Splash Potion
				if (potionAbsorp > -1 && potionAbsorp > playerAbsorp) candidates.add(i)

				// Health Boost Splash Potion
				if (potionHealthboost > -1 && potionHealthboost > playerHealthBoost) candidates.add(i)

				// Night Vision Splash Potion
				if (potionNightVision && !playerNightVision) candidates.add(i)

				// Water Breathing Splash Potion
				if (potionWaterBreath > -1 && potionWaterBreath > playerWaterBreath) candidates.add(i)

				// Jump Boost Splash Potion
				if (jumpPot && potionJump > -1 && potionJump > playerJump) candidates.add(i)

				// Invisibility Splash Potion
				if (invisPot && potionInvis && !playerInvis) candidates.add(i)
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
