/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState.POST
import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.Container
import net.minecraft.item.ItemPotion
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import java.util.*
import kotlin.random.Random

// TODO: Movement prediction when throwing pot
@ModuleInfo(name = "AutoPot", description = "Automatically throws healing potions.", category = ModuleCategory.COMBAT)
class AutoPot : Module()
{
    private val healthValue = FloatValue("Health", 15F, 1F, 20F)

    private val throwGroup = ValueGroup("Throw")
    private val throwModeValue = ListValue("Mode", arrayOf("Normal", "Jump", "Port"), "Normal")
    private val throwDirectionValue = ListValue("ThrowDirection", arrayOf("Up", "Down"), "Down")
    private val throwDelayValue = IntegerRangeValue("Delay", 250, 250, 0, 2000, "MaxPotDelay" to "MinPotDelay")
    private val throwSilentValue = BoolValue("Silent", true)
    private val throwNoMoveThrowValue = BoolValue("NoMove", false, "NoMove-Throw")
    private val throwRandomSlotValue = BoolValue("RandomSlot", false)
    private val throwGroundDistanceValue = FloatValue("GroundDistance", 2F, 0.72F, 5F)
    private val throwIgnoreScreenValue = BoolValue("IgnoreScreen", true)

    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

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
    private val killauraBypassModeValue = ListValue("Mode", arrayOf("None", "SuspendKillAura", "WaitForKillAuraEnd"), "SuspendKillAura", "KillAuraBypassMode")
    private val killAuraBypassKillAuraSuspendDurationValue = object : IntegerValue("Duration", 300, 100, 1000, "SuspendKillAuraDuration")
    {
        override fun showCondition() = killauraBypassModeValue.get().equals("SuspendKillAura", ignoreCase = true)
    }

    private val potionFilterGroup = ValueGroup("PotionFilter")
    private val potionFilterInvisibleValue = BoolValue("InvisibilityPot", false, "InvisibilityPot")

    private val potionFilterJumpBoostGroup = ValueGroup("JumpBoost")
    private val potionFilterJumpBoostEnabledValue = BoolValue("Enabled", true, "JumpBoostPot")
    private val potionFilterJumpBoostAmpLimitValue = IntegerValue("AmplifierLimit", 5, 2, 127, "JumpPotAmplifierLimit")

    private val potThrowDelayTimer = MSTimer()
    private var potThrowDelay = throwDelayValue.getRandomLong()
    private var invDelay = inventoryDelayValue.getRandomLong()

    private var potion = -1

    init
    {
        throwGroup.addAll(throwModeValue, throwDirectionValue, throwDelayValue, throwSilentValue, throwNoMoveThrowValue, throwRandomSlotValue, throwGroundDistanceValue, throwIgnoreScreenValue)

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
        val goodEffects = hashSetOf(Potion.absorption.id, Potion.damageBoost.id, Potion.digSpeed.id, Potion.heal.id, Potion.healthBoost.id, Potion.invisibility.id, Potion.jump.id, Potion.moveSpeed.id, Potion.nightVision.id, Potion.regeneration.id, Potion.resistance.id, Potion.waterBreathing.id, Potion.fireResistance.id)

        @JvmStatic
        fun isPotionUseful(item: ItemStack): Boolean
        {
            val potionItem = item.item ?: return false

            if (potionItem !is ItemPotion) return false

            return potionItem.getEffects(item).any { goodEffects.contains(it.potionID) }
        }
    }

    @EventTarget
    fun onMotion(motionEvent: MotionEvent)
    {
        val controller = mc.playerController

        if (controller.isInCreativeMode) return

        val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
        if (killauraBypassModeValue.get().equals("WaitForKillAuraEnd", true) && killAura.state && killAura.target != null) return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val screen = mc.currentScreen
        val netHandler = mc.netHandler

        val inventoryContainer = thePlayer.inventoryContainer
        val activePotionEffects = thePlayer.activePotionEffects

        val randomSlot = throwRandomSlotValue.get()
        val invRandomSlot = inventoryRandomSlotValue.get()
        val throwDirection = throwDirectionValue.get().lowercase(Locale.getDefault())
        val health = healthValue.get()

        val containerOpen = screen is GuiContainer
        val isNotInventory = screen !is GuiInventory

        val serverRotation = RotationUtils.serverRotation
        val ignoreScreen = throwIgnoreScreenValue.get()

        when (motionEvent.eventState)
        {
            PRE ->
            {
                if (potThrowDelayTimer.hasTimePassed(potThrowDelay) && (ignoreScreen || containerOpen) && !(throwNoMoveThrowValue.get() && thePlayer.isMoving))
                {
                    // Hotbar Potion
                    val healPotionInHotbar = findHealPotion(thePlayer, 36, 45, inventoryContainer, randomSlot)
                    val buffPotionInHotbar = findBuffPotion(activePotionEffects, 36, 45, inventoryContainer, randomSlot)

                    if (thePlayer.health <= health && healPotionInHotbar != -1 || buffPotionInHotbar != -1)
                    {
                        if (thePlayer.onGround)
                        {
                            when (throwModeValue.get().lowercase(Locale.getDefault()))
                            {
                                "jump" -> thePlayer.jump()
                                "port" -> thePlayer.moveEntity(0.0, 0.42, 0.0)
                            }
                        }

                        val posY = thePlayer.posY

                        // Prevent throwing potions into the void
                        val fallingPlayer = FallingPlayer(theWorld, thePlayer, thePlayer.posX + thePlayer.motionX * 2, posY, thePlayer.posZ + thePlayer.motionZ * 2, thePlayer.motionX, thePlayer.motionY, thePlayer.motionZ, thePlayer.rotationYaw, thePlayer.moveStrafing, thePlayer.moveForward)

                        val collisionBlock = fallingPlayer.findCollision(20)?.pos

                        if (posY - (collisionBlock?.y ?: 0) >= throwGroundDistanceValue.get()) return

                        // Suspend killaura if option is present
                        if (killauraBypassModeValue.get().equals("SuspendKillAura", true)) killAura.suspend(killAuraBypassKillAuraSuspendDurationValue.get().toLong())

                        potion = if (thePlayer.health <= health && healPotionInHotbar != -1) healPotionInHotbar else buffPotionInHotbar

                        val potionIndex = potion - 36

                        if (throwSilentValue.get())
                        {
                            if (!InventoryUtils.tryHoldSlot(thePlayer, potionIndex, -1, true)) return
                        }
                        else
                        {
                            thePlayer.inventory.currentItem = potionIndex
                            controller.updateController()
                        }

                        // // Swap hotbar slot to potion slot
                        // netHandler.addToSendQueue(C09PacketHeldItemChange(potion - 36))

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
                if (InventoryUtils.CLICK_TIMER.hasTimePassed(invDelay) && !(inventoryNoMoveValue.get() && thePlayer.isMoving) && !(currentContainer != null && currentContainer.windowId != 0))
                {
                    // Move Potion Inventory -> Hotbar
                    val healPotionInInventory = findHealPotion(thePlayer, 9, 36, inventoryContainer, invRandomSlot)
                    val buffPotionInInventory = findBuffPotion(activePotionEffects, 9, 36, inventoryContainer, invRandomSlot)

                    if ((healPotionInInventory != -1 || buffPotionInInventory != -1) && thePlayer.inventory.hasSpaceHotbar)
                    {
                        if (inventoryOpenInventoryValue.get() && isNotInventory) return

                        var slot = if (healPotionInInventory != -1) healPotionInInventory else buffPotionInInventory

                        val misclickRate = inventoryMisclickRateValue.get()

                        // Simulate Click Mistakes to bypass some (geek) anti-cheat's click accuracy checks
                        if (inventoryMisclickEnabledValue.get() && misclickRate > 0 && Random.nextInt(100) <= misclickRate)
                        {
                            val firstEmpty = thePlayer.inventoryContainer.firstEmpty(9, 36, invRandomSlot)
                            if (firstEmpty != -1) slot = firstEmpty
                        }

                        val openInventory = isNotInventory && inventorySimulateInventoryValue.get()

                        if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

                        controller.windowClick(0, slot, 0, 1, thePlayer)

                        if (openInventory) netHandler.addToSendQueue(C0DPacketCloseWindow())

                        invDelay = inventoryDelayValue.getRandomLong()
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
                        netHandler.addToSendQueue(createUseItemPacket(itemStack))

                        if (throwSilentValue.get()) InventoryUtils.resetSlot(thePlayer)

                        potThrowDelay = throwDelayValue.getRandomLong()
                        potThrowDelayTimer.reset()
                    }

                    potion = -1
                }
            }
        }
    }

    private fun findHealPotion(thePlayer: EntityLivingBase, startSlot: Int, endSlot: Int, inventoryContainer: Container, random: Boolean, splash: Boolean = true): Int
    {
        val candidates = mutableListOf<Int>()

        val regenPotion = Potion.regeneration

        val healID = Potion.heal.id
        val regenID = regenPotion.id

        val playerRegen = thePlayer.isPotionActive(regenPotion)

        (startSlot until endSlot).mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { it.second.item is ItemPotion }.run { if (splash) filter { ItemPotion.isSplash(it.second.metadata) } else filterNot { ItemPotion.isSplash(it.second.metadata) } }.forEach { (slotIndex, stack) ->
            var heal = false
            var regen = false

            ((stack.item ?: return@forEach) as ItemPotion).getEffects(stack).map(PotionEffect::getPotionID).forEach { potionID ->
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

    fun findBuffPotion(activePotionEffects: Collection<PotionEffect>, startSlot: Int, endSlot: Int, inventoryContainer: Container, random: Boolean, splash: Boolean = true, itemDelay: Long = itemDelayValue.get().toLong()): Int
    {
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

        val speedID = Potion.moveSpeed.id
        val jumpID = Potion.jump.id
        val hasteID = Potion.digSpeed.id
        val strengthID = Potion.damageBoost.id
        val fireResisID = Potion.fireResistance.id
        val resisID = Potion.resistance.id
        val absorptionID = Potion.absorption.id
        val healthBoostID = Potion.healthBoost.id
        val waterBreathID = Potion.waterBreathing.id
        val invisID = Potion.invisibility.id
        val nightVisionID = Potion.nightVision.id

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

        (startSlot until endSlot).asSequence().mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { currentTime - it.second.itemDelay >= itemDelay }.filter { it.second.item is ItemPotion }.run { if (splash) filter { ItemPotion.isSplash(it.second.metadata) } else filterNot { ItemPotion.isSplash(it.second.metadata) } }.forEach { (slotIndex, stack) ->
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

            ((stack.item ?: return@forEach) as ItemPotion).getEffects(stack).map { it.potionID to it.amplifier }.forEach { (potionID, amplifier) ->
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
            // </editor-fold>

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
