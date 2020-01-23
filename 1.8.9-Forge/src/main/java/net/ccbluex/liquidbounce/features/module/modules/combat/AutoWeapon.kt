/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C09PacketHeldItemChange

@ModuleInfo(name = "AutoWeapon", description = "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT)
class AutoWeapon : Module() {
    private val silentValue = BoolValue("SpoofItem", false)

    private var packetUseEntity: C02PacketUseEntity? = null
    private var spoofedSlot = false
    private var gotIt = false
    private var tick = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        gotIt = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C02PacketUseEntity && event.packet.action == C02PacketUseEntity.Action.ATTACK && gotIt) {
            gotIt = false

            var slot = -1
            var bestDamage = 0.0

            for (i in 0..8) {
                val itemStack = mc.thePlayer.inventory.getStackInSlot(i)

                if (itemStack != null && (itemStack.item is ItemSword || itemStack.item is ItemTool)) {
                    for (attributeModifier in itemStack.attributeModifiers["generic.attackDamage"]) {
                        val damage = attributeModifier.amount + 1.25 *
                                ItemUtils.getEnchantment(itemStack, Enchantment.sharpness)

                        if (damage > bestDamage) {
                            bestDamage = damage
                            slot = i
                        }
                    }
                }
            }

            if (slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
                if (silentValue.get()) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
                    spoofedSlot = true
                } else {
                    mc.thePlayer.inventory.currentItem = slot
                    mc.playerController.updateController()
                }

                event.cancelEvent()
                packetUseEntity = event.packet
                tick = 0
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: MotionEvent) {
        if (tick < 1) {
            tick++
            return
        }

        if (packetUseEntity != null) {
            mc.netHandler.networkManager.sendPacket(packetUseEntity)

            if (spoofedSlot)
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))

            packetUseEntity = null
        }
    }
}