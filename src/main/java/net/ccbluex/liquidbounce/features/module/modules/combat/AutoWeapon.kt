/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.InventoryUtils.sendSlotChange
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK

object AutoWeapon : Module("AutoWeapon", ModuleCategory.COMBAT) {

    private val spoof by BoolValue("SpoofItem", false)
    private val spoofTicks by IntegerValue("SpoofTicks", 10, 1..20) { spoof }

    private var attackEnemy = false

    private var ticks = 0

    // Find the best weapon in hotbar (#Kotlin Style)
    val slot
        get() = (0..8).map {
            Pair(
                it, mc.thePlayer.inventory.getStackInSlot(it)
            )
        }.filter { it.second != null && (it.second.item is ItemSword || it.second.item is ItemTool) }.maxByOrNull {
            (it.second.attributeModifiers["generic.attackDamage"].first()?.amount
                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(
                it.second, Enchantment.sharpness
            )
        }?.first

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C02PacketUseEntity && event.packet.action == ATTACK && attackEnemy) {
            attackEnemy = false

            val slot = slot ?: return

            // If in hand no need to swap
            if (slot == mc.thePlayer.inventory.currentItem) return

            // Switch to best weapon
            if (spoof) {
                sendPacket(sendSlotChange(mc.thePlayer.inventory.currentItem, slot))
                ticks = spoofTicks
            } else {
                mc.thePlayer.inventory.currentItem = slot
                mc.playerController.updateController()
            }

            // Resend attack packet
            sendPacket(event.packet)
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(update: UpdateEvent) {
        // Switch back to old item after some time
        if (ticks > 0) {
            if (ticks == 1) sendPacket(
                sendSlotChange(
                    slot ?: -1, mc.thePlayer.inventory.currentItem
                )
            )
            ticks--
        }
    }
}