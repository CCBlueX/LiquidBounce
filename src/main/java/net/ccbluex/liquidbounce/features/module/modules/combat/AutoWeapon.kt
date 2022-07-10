/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.extensions.getEnchantmentLevel
import net.ccbluex.liquidbounce.utils.extensions.itemDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity

@ModuleInfo(name = "AutoWeapon", description = "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT)
class AutoWeapon : Module()
{
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)
    private val onlySwordValue = BoolValue("OnlySword", false)

    private val silentGroup = ValueGroup("Silent")
    private val silentEnabledValue = BoolValue("Enabled", false, "SpoofItem")
    private val silentKeepTicksValue = IntegerValue("KeepTicks", 10, 0, 20, "SpoofTicks")

    private var attackEnemy = false

    init
    {
        silentGroup.addAll(silentEnabledValue, silentKeepTicksValue)
    }

    @EventTarget
    fun onAttack(@Suppress("UNUSED_PARAMETER") event: AttackEvent)
    {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet !is C02PacketUseEntity) return

        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler

        if (packet.action == C02PacketUseEntity.Action.ATTACK && attackEnemy)
        {
            val inventory = thePlayer.inventory

            attackEnemy = false

            val itemDelay = itemDelayValue.get()
            val onlySword = onlySwordValue.get()

            val currentTime = System.currentTimeMillis()

            // Find best weapon in hotbar (#Kotlin Style)
            val (slot, _) = (0..8).asSequence().mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, stack) -> (stack.item is ItemSword || !onlySword && stack.item is ItemTool) && currentTime - stack.itemDelay >= itemDelay }.maxByOrNull { (_, stack) -> (stack.attributeModifiers["generic.attackDamage"].firstOrNull()?.amount ?: 2.0) + 1.25 * stack.getEnchantmentLevel(Enchantment.sharpness) } ?: return

            if (slot == inventory.currentItem) // If in hand no need to swap
                return

            // Switch to best weapon
            if (silentEnabledValue.get())
            {
                if (!InventoryUtils.tryHoldSlot(thePlayer, slot, silentKeepTicksValue.get())) return
            }
            else
            {
                inventory.currentItem = slot
                mc.playerController.updateController()
            }

            // Resend attack packet
            netHandler.addToSendQueue(packet)
            event.cancelEvent()
        }
    }

    override val tag: String?
        get() = if (silentEnabledValue.get()) "Silent" else null
}
