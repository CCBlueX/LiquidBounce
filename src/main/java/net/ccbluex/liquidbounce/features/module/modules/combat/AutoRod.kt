package net.ccbluex.liquidbounce.features.module.modules.combat

import com.google.common.collect.Multimap
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EnumCreatureAttribute
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

@ModuleInfo(name = "AutoRod", description = "Auto use fishing rod to PVP", category = ModuleCategory.COMBAT)
class AutoRod: Module() {
    private val t1 = MSTimer()
    private val t2 = MSTimer()

    private var switchBack = false
    private var useRod = false

    private val delay = FloatValue("Delay", 100f, 50f, 1000f)
    private val disable = BoolValue("AutoDisable", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val item = Item.getIdFromItem((mc.thePlayer.heldItem ?: return).item)
        val rodDelay = delay.get()
        if (mc.currentScreen != null) {
            return
        }
        if (!disable.get()) {
            if (!useRod && item == 346) {
                rod()
                useRod = true
            }
            if (t1.hasTimePassed((rodDelay - 50).toLong()) && switchBack) {
                switchBack()
                switchBack = false
            }
            if (t1.hasTimePassed(rodDelay.toLong()) && useRod) {
                useRod = false
            }
        } else {
            if (item == 346) {
                if (t2.hasTimePassed((rodDelay + 200).toLong())) {
                    rod()
                    t2.reset()
                }
                if (t1.hasTimePassed(rodDelay.toLong())) {
                    mc.thePlayer.inventory.currentItem = bestWeapon()
                    t1.reset()
                    toggle()
                }
            } else if (t1.hasTimePassed(100)) {
                switchToRod()
                t1.reset()
            }
        }
    }

    private fun findRod(startSlot: Int, endSlot: Int): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item === Items.fishing_rod) {
                return i
            }
        }
        return -1
    }

    private fun switchToRod() {
        for (i in 36..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && Item.getIdFromItem(stack.item) == 346) {
                mc.thePlayer.inventory.currentItem = i - 36
                break
            }
        }
    }

    private fun rod() {
        val rod = findRod(36, 45)
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventoryContainer.getSlot(rod).stack)
        switchBack = true
        t1.reset()
    }

    private fun switchBack() {
        mc.thePlayer.inventory.currentItem = bestWeapon()
    }

    private fun bestWeapon(): Int {
        mc.thePlayer.inventory.currentItem = 0
        val firstSlot = mc.thePlayer.inventory.currentItem
        var bestWeapon = -1
        var j = 1
        for (i in 0..8) {
            mc.thePlayer.inventory.currentItem = i
            val itemStack = mc.thePlayer.heldItem
            if (itemStack != null) {
                var itemAtkDamage = getItemAtkDamage(itemStack).toInt()
                itemAtkDamage += EnchantmentHelper.getModifierForCreature(itemStack, EnumCreatureAttribute.UNDEFINED)
                    .toInt()
                if (itemAtkDamage > j) {
                    j = itemAtkDamage
                    bestWeapon = i
                }
            }
        }
        return if (bestWeapon != -1) {
            bestWeapon
        } else {
            firstSlot
        }
    }


    private fun getItemAtkDamage(itemStack: ItemStack): Float {
        val multimap: Multimap<*, *> = itemStack.attributeModifiers
        if (!multimap.isEmpty) {
            val iterator: Iterator<*> = multimap.entries().iterator()
            if (iterator.hasNext()) {
                val (_, value) = iterator.next() as Map.Entry<*, *>
                val attributeModifier = value as AttributeModifier
                val damage =
                    if (attributeModifier.operation != 1 && attributeModifier.operation != 2) attributeModifier.amount else attributeModifier.amount * 100.0
                return if (attributeModifier.amount > 1.0) {
                    1.0f + damage.toFloat()
                } else 1.0f
            }
        }
        return 1.0f
    }
}