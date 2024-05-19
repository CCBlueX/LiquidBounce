/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

object AutoPlay : Module("AutoPlay", Category.PLAYER, gameDetecting = false, hideModule = false) {

    private val mode by ListValue("Mode", arrayOf("Paper", "Hypixel"), "Paper")

    // Hypixel Settings
    private val hypixelMode by ListValue("HypixelMode", arrayOf("Skywars", "Bedwars"), "Skywars") {
        mode == "Hypixel"
    }
    private val skywarsMode by ListValue("SkywarsMode", arrayOf("SoloNormal", "SoloInsane"), "SoloNormal") {
        hypixelMode == "Skywars"
    }
    private val bedwarsMode by ListValue("BedwarsMode", arrayOf("Solo", "Double", "Trio", "Quad"), "Solo") {
        hypixelMode == "Bedwars"
    }

    private val delay by IntegerValue("Delay", 50, 0..200)

    private var delayTick = 0

    /**
     * Update Event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        player ?: return

        if (!playerInGame() || !player.inventory.hasItemStack(ItemStack(Items.paper))) {
            if (delayTick > 0)
                delayTick = 0

            return
        } else {
            delayTick++
        }

        when (mode) {
            "Paper" -> {
                val paper = findPaper()

                if (paper == -1) {
                    return
                }

                player.inventory.currentItem = (paper - 36)
                mc.playerController.updateController()

                if (delayTick >= delay) {
                    mc.playerController.sendUseItem(player, mc.theWorld, player.inventoryContainer.getSlot(paper).stack)
                    delayTick = 0
                }
            }

            "Hypixel" -> {
                if (delayTick >= delay) {
                    if (hypixelMode == "Skywars") {
                        when (skywarsMode) {
                            "SoloNormal" -> player.sendChatMessage("/play solo_normal")
                            "SoloInsane" -> player.sendChatMessage("/play solo_insane")
                        }
                    } else {
                        when (bedwarsMode) {
                            "Solo" -> player.sendChatMessage("/play bedwars_eight_one")
                            "Double" -> player.sendChatMessage("/play bedwars_eight_two")
                            "Trio" -> player.sendChatMessage("/play bedwars_four_three")
                            "Quad" -> player.sendChatMessage("/play bedwars_four_four")
                        }
                    }
                    delayTick = 0
                }
            }
        }
    }

    /**
     * Find paper in inventory
     */
    private fun findPaper(): Int {
        for (i in 36 until 45) {
            val stack = player?.inventoryContainer?.getSlot(i)?.stack
            if (stack != null) {
                if (stack.item == Items.paper) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Check whether player is in game or not
     */
    private fun playerInGame(): Boolean {
        player ?: return false

        return player.ticksExisted > 10
                && (player.capabilities.isFlying
                || player.capabilities.allowFlying
                || player.capabilities.disableDamage)
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = mode
}
