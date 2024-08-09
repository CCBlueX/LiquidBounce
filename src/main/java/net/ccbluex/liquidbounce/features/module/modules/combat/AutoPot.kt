/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState.POST
import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.isSplashPotion
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.potion.Potion

object AutoPot : Module("AutoPot", Category.COMBAT, hideModule = false) {

    private val health by FloatValue("Health", 15F, 1F..20F) { healPotion || regenerationPotion }
    private val delay by IntegerValue("Delay", 500, 500..1000)

    // Useful potion options
    private val healPotion by BoolValue("HealPotion", true)
    private val regenerationPotion by BoolValue("RegenPotion", true)
    private val fireResistancePotion by BoolValue("FireResPotion", true)
    private val strengthPotion by BoolValue("StrengthPotion", true)
    private val jumpPotion by BoolValue("JumpPotion", true)
    private val speedPotion by BoolValue("SpeedPotion", true)

    private val openInventory by BoolValue("OpenInv", false)
    private val simulateInventory by BoolValue("SimulateInventory", true) { !openInventory }

    private val groundDistance by FloatValue("GroundDistance", 2F, 0F..5F)
    private val mode by ListValue("Mode", arrayOf("Normal", "Jump", "Port"), "Normal")

    private val msTimer = MSTimer()
    private var potion = -1

    @EventTarget
    fun onMotion(motionEvent: MotionEvent) {
        if (!msTimer.hasTimePassed(delay) || mc.playerController.isInCreativeMode)
            return

        val player = mc.thePlayer ?: return

        when (motionEvent.eventState) {
            PRE -> {
                // Hotbar Potion
                val potionInHotbar = findPotion(36, 45)

                if (potionInHotbar != null) {
                    if (player.onGround) {
                        when (mode.lowercase()) {
                            "jump" -> player.tryJump()
                            "port" -> player.moveEntity(0.0, 0.42, 0.0)
                        }
                    }

                    // Prevent throwing potions into the void
                    val fallingPlayer = FallingPlayer(player)

                    val collisionBlock = fallingPlayer.findCollision(20)?.pos

                    if (player.posY - (collisionBlock?.y ?: return) - 1 > groundDistance)
                        return

                    potion = potionInHotbar
                    sendPacket(C09PacketHeldItemChange(potion - 36))

                    if (player.rotationPitch <= 80F) {
                        setTargetRotation(Rotation(player.rotationYaw, nextFloat(80F, 90F)).fixedSensitivity(),
                            immediate = true
                        )
                    }
                    return
                }

                // Inventory Potion -> Hotbar Potion
                val potionInInventory = findPotion(9, 36) ?: return
                if (InventoryUtils.hasSpaceInHotbar()) {
                    if (openInventory && mc.currentScreen !is GuiInventory)
                        return

                    if (simulateInventory)
                        serverOpenInventory = true

                    mc.playerController.windowClick(0, potionInInventory, 0, 1, player)

                    if (simulateInventory && mc.currentScreen !is GuiInventory)
                        serverOpenInventory = false

                    msTimer.reset()
                }
            }

            POST -> {
                if (potion >= 0 && serverRotation.pitch >= 75F) {
                    val itemStack = player.inventoryContainer.getSlot(potion).stack

                    if (itemStack != null) {
                        sendPackets(
                            C08PacketPlayerBlockPlacement(itemStack),
                            C09PacketHeldItemChange(player.inventory.currentItem)
                        )

                        msTimer.reset()
                    }

                    potion = -1
                }
            }
        }
    }

    private fun findPotion(startSlot: Int, endSlot: Int): Int? {
        val player = mc.thePlayer

        for (i in startSlot until endSlot) {
            val stack = player.inventoryContainer.getSlot(i).stack

            if (stack == null || stack.item !is ItemPotion || !stack.isSplashPotion())
                continue

            val itemPotion = stack.item as ItemPotion

            for (potionEffect in itemPotion.getEffects(stack))
                if (player.health <= health && healPotion && potionEffect.potionID == Potion.heal.id)
                    return i

            if (!player.isPotionActive(Potion.regeneration))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (player.health <= health && regenerationPotion && potionEffect.potionID == Potion.regeneration.id)
                        return i

            if (!player.isPotionActive(Potion.fireResistance))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (fireResistancePotion && potionEffect.potionID == Potion.fireResistance.id)
                        return i

            if (!player.isPotionActive(Potion.moveSpeed))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (speedPotion && potionEffect.potionID == Potion.moveSpeed.id)
                        return i

            if (!player.isPotionActive(Potion.jump))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (jumpPotion && potionEffect.potionID == Potion.jump.id)
                        return i

            if (!player.isPotionActive(Potion.damageBoost))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (strengthPotion && potionEffect.potionID == Potion.damageBoost.id)
                        return i
        }

        return null
    }

    override val tag
        get() = health.toString()

}