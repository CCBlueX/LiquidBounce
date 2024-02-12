/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object AutoPlay : Module("AutoPlay", ModuleCategory.PLAYER, gameDetecting = false) {

    private val mode by ListValue("Mode", arrayOf("BlocksMC", "HypixelSkywars"), "BlocksMC")

    // Hypixel Skywars AutoPlay Settings
    private val skywarsMode by ListValue("SkywarsMode", arrayOf("Normal", "Insane"), "Normal") {
        mode == "HypixelSkywars"
    }

    private val delay by IntegerValue("Delay", 50, 0..200)

    private var delayTick = 0

    /**
     * Update Event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (!player.isAirBorne || !player.inventory.hasItemStack(ItemStack(Items.paper))) {
            return
        } else {
            delayTick++
        }

        when (mode) {
            "BlocksMC" -> {
                val paper = findPaper(36, 45)

                if (paper == -1) {
                    return
                }

                mc.thePlayer.inventory.currentItem = (paper - 36)
                mc.playerController.updateController()

                if (delayTick >= delay) {
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventoryContainer.getSlot(paper).stack)
                    delayTick = 0
                }
            }

            "HypixelSkywars" -> {
                if (delayTick >= delay) {
                    when (skywarsMode) {
                        "Normal" -> player.sendChatMessage("/play solo_normal")
                        "Insane" -> player.sendChatMessage("/play solo_insane")
                    }
                    delayTick = 0
                }
            }

            "MinemenClub" -> {
                if (player.ticksExisted % 15 == 1) {
                    val paper = findPaper(36, 45)
                    if (paper == -1) return

                    mc.thePlayer.rotationPitch = -90f
                    mc.thePlayer.inventory.currentItem = (paper - 36)
                    mc.playerController.updateController()
                    if (delayTick >= delay) {
                        mc.rightClickMouse()
                    }
                }
            }
        }
    }

    /**
     * Find paper in inventory
     */
    private fun findPaper(startSlot: Int, endSlot: Int): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.thePlayer?.inventoryContainer?.getSlot(i)?.stack
            if (stack != null) {
                if (stack.item == Items.paper) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = mode
}