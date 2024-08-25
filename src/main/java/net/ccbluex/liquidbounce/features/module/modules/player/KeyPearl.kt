/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.TextValue
import net.minecraft.init.Items
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object KeyPearl : Module("KeyPearl", Category.PLAYER, subjective = true, gameDetecting = false, hideModule = false) {

    private val delayedSlotSwitch by BoolValue("DelayedSlotSwitch", true)
    private val mouse by BoolValue("Mouse", false)
        private val mouseButtonValue = ListValue("MouseButton",
            arrayOf("Left", "Right", "Middle", "MouseButton4", "MouseButton5"), "Middle") { mouse }

        private val keyName by TextValue("KeyName", "X") { !mouse }

    private val noEnderPearlsMessage by BoolValue("NoEnderPearlsMessage", true)

    private var wasMouseDown = false
    private var wasKeyDown = false
    private var hasThrown = false

    private fun throwEnderPearl() {
        val pearlInHotbar = InventoryUtils.findItem(36, 44, Items.ender_pearl)

        if (pearlInHotbar == null) {
            if (noEnderPearlsMessage) {
                displayChatMessage("§6§lWarning: §aThere are no ender pearls in your hotbar.")
            }
            return
        }

        // don't wait before and after throwing if the player is already holding an ender pearl
        if (!delayedSlotSwitch || mc.player.inventory.currentItem == pearlInHotbar - 36) {
            sendPackets(
                C09PacketHeldItemChange(pearlInHotbar - 36),
                C08PacketPlayerBlockPlacement(mc.player.heldItem),
                C09PacketHeldItemChange(mc.player.inventory.currentItem))
            return
        }

        sendPackets(
            C09PacketHeldItemChange(pearlInHotbar - 36),
            C08PacketPlayerBlockPlacement(mc.player.heldItem))
        hasThrown = true
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        if (hasThrown) {
            sendPackets(C09PacketHeldItemChange(mc.player.inventory.currentItem))
            hasThrown = false
            
        }

        if (mc.currentScreen != null || mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR
            || mc.playerController.currentGameType == WorldSettings.GameType.CREATIVE) return
			
		val isMouseDown = Mouse.isButtonDown(mouseButtonValue.values.indexOf(mouseButtonValue.get()))
		val isKeyDown = Keyboard.isKeyDown(Keyboard.getKeyIndex(keyName.uppercase()))

        if (mouse && !wasMouseDown && isMouseDown) {
            throwEnderPearl()
        } else if (!mouse && !wasKeyDown && isKeyDown) {
            throwEnderPearl()
        }

        wasMouseDown = isMouseDown
        wasKeyDown = isKeyDown
    }

    override fun onEnable() {
        hasThrown = false
    }
}