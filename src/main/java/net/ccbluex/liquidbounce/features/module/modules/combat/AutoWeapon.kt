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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C09PacketHeldItemChange

@ModuleInfo(name = "AutoWeapon", description = "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT)
class AutoWeapon : Module() {

    private val silentValue = BoolValue("SpoofItem", false)
    private val ticksValue = IntegerValue("SpoofTicks", 10, 1, 20)
    private var attackEnemy = false

    private var spoofedSlot = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C02PacketUseEntity && event.packet.action == C02PacketUseEntity.Action.ATTACK
            && attackEnemy) {
            attackEnemy = false

            // Find best weapon in hotbar (#Kotlin Style)
            val (slot, _) = (0..8)
                .map { Pair(it, mc.thePlayer.inventory.getStackInSlot(it)) }
                .filter { it.second != null && (it.second.item is ItemSword || it.second.item is ItemTool) }
                .maxByOrNull {
                    (it.second.attributeModifiers["generic.attackDamage"].first()?.amount
                        ?: 0.0) + 1.25 * ItemUtils.getEnchantment(it.second, Enchantment.sharpness)
                } ?: return

            if (slot == mc.thePlayer.inventory.currentItem) // If in hand no need to swap
                return

            // Switch to best weapon
            if (silentValue.get()) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
                spoofedSlot = ticksValue.get()
            } else {
                mc.thePlayer.inventory.currentItem = slot
                mc.playerController.updateController()
            }

            // Resend attack packet
            mc.netHandler.addToSendQueue(event.packet)
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(update: UpdateEvent) {
        // Switch back to old item after some time
        if (spoofedSlot > 0) {
            if (spoofedSlot == 1)
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            spoofedSlot--
        }
    }
}