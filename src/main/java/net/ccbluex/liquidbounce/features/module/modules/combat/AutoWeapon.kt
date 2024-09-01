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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.inventory.attackDamage
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.item.SwordItem
import net.minecraft.item.ToolItem
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Action.ATTACK

object AutoWeapon : Module("AutoWeapon", Category.COMBAT, subjective = true, hideModule = false) {

    private val onlySword by BoolValue("OnlySword", false)

    private val spoof by BoolValue("SpoofItem", false)
        private val spoofTicks by IntegerValue("SpoofTicks", 10, 1..20) { spoof }

    private var attackEnemy = false

    private var ticks = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerInteractEntityC2SPacket && event.packet.action == ATTACK && attackEnemy) {
            attackEnemy = false

            // Find the best weapon in hotbar (#Kotlin Style)
            val (slot, _) = (0..8)
                .map { it to mc.player.inventory.getInvStack(it) }
                .filter { it.second != null && ((onlySword && it.second.item is SwordItem)
                        || (!onlySword && (it.second.item is SwordItem || it.second.item is ToolItem))) }
                .maxByOrNull { it.second.attackDamage } ?: return

            if (slot == mc.player.inventory.selectedSlot) // If in hand no need to swap
                return

            // Switch to best weapon
            if (spoof) {
                serverSlot = slot
                ticks = spoofTicks
            } else {
                mc.player.inventory.selectedSlot = slot
               mc.interactionManager.syncSelectedSlot()
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
            if (ticks == 1)
                serverSlot = mc.player.inventory.selectedSlot

            ticks--
        }
    }
}