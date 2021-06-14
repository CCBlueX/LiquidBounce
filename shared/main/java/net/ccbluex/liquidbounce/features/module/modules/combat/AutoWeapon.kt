/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnchantmentType
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "AutoWeapon", description = "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT)
class AutoWeapon : Module()
{
	private val silentValue = BoolValue("SpoofItem", false) // Silent
	private val silentKeepTicksValue = IntegerValue("SpoofTicks", 10, 0, 20) // SilentKeepTicks
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 1000)
	private val onlySwordValue = BoolValue("OnlySword", false)

	private var attackEnemy = false

	@EventTarget
	fun onAttack(@Suppress("UNUSED_PARAMETER") event: AttackEvent)
	{
		attackEnemy = true
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val provider = classProvider

		if (!provider.isCPacketUseEntity(event.packet)) return

		val thePlayer = mc.thePlayer ?: return
		val netHandler = mc.netHandler

		val packet = event.packet.asCPacketUseEntity()

		if (packet.action == ICPacketUseEntity.WAction.ATTACK && attackEnemy)
		{
			val inventory = thePlayer.inventory

			attackEnemy = false

			val itemDelay = itemDelayValue.get()
			val onlySword = onlySwordValue.get()

			val currentTime = System.currentTimeMillis()

			// Find best weapon in hotbar (#Kotlin Style)
			val (slot, _) = (0..8).asSequence().mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, stack) -> (provider.isItemSword(stack.item) || !onlySword && provider.isItemTool(stack.item)) && currentTime - stack.itemDelay >= itemDelay }.maxBy { (_, stack) -> (stack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 2.0) + 1.25 * ItemUtils.getEnchantment(stack, provider.getEnchantmentEnum(EnchantmentType.SHARPNESS)) } ?: return

			if (slot == inventory.currentItem) // If in hand no need to swap
				return

			// Switch to best weapon
			if (silentValue.get())
			{
				if (InventoryUtils.setHeldItemSlot(slot, silentKeepTicksValue.get())) return
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
		get() = if (silentValue.get()) "Silent" else null
}
