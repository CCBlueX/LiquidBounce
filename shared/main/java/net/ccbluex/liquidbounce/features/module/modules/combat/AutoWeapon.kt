/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnchantmentType
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue

@ModuleInfo(name = "AutoWeapon", description = "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT)
class AutoWeapon : Module()
{

	private val silentValue = BoolValue("SpoofItem", false)
	private val ticksValue = IntegerValue("SpoofTicks", 10, 1, 20)
	private var attackEnemy = false

	private var spoofedSlot = 0

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
			attackEnemy = false

			// Find best weapon in hotbar (#Kotlin Style)
			val (slot, _) = (0..8).asSequence().mapNotNull { it to (thePlayer.inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (_, itemStack) -> provider.isItemSword(itemStack.item) || provider.isItemTool(itemStack.item) }.maxBy { (_, itemStack) ->
				itemStack.getAttributeModifier("generic.attackDamage").first().amount + 1.25 * ItemUtils.getEnchantment(itemStack, provider.getEnchantmentEnum(EnchantmentType.SHARPNESS))
			} ?: return

			if (slot == thePlayer.inventory.currentItem) // If in hand no need to swap
				return

			// Switch to best weapon
			if (silentValue.get())
			{
				netHandler.addToSendQueue(provider.createCPacketHeldItemChange(slot))
				spoofedSlot = ticksValue.get()
			}
			else
			{
				thePlayer.inventory.currentItem = slot
				mc.playerController.updateController()
			}

			// Resend attack packet
			netHandler.addToSendQueue(packet)
			event.cancelEvent()
		}
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") update: UpdateEvent)
	{

		// Switch back to old item after some time
		if (spoofedSlot > 0)
		{
			if (spoofedSlot == 1) mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
			spoofedSlot--
		}
	}

	override val tag: String?
		get() = if (silentValue.get()) "Silent" else null
}
