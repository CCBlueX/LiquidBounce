package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
@ModuleInfo(name = "MurderDetector", description = "Detects murder in murder mystery.", category = ModuleCategory.MISC)
class MurderDetector : Module()
{
	val murders = mutableListOf<IEntityPlayer>()

	override fun onEnable()
	{
		murders.clear()
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		theWorld.loadedEntityList.asSequence().filter { classProvider.isEntityPlayer(it) && it != mc.thePlayer && it.asEntityPlayer().currentEquippedItem != null && it.asEntityPlayer().currentEquippedItem!!.item != null && isMurder(it.asEntityPlayer().currentEquippedItem!!.item!!) && !murders.contains(it) }.forEach {
			murders.add(it.asEntityPlayer())
			ClientUtils.displayChatMessage(thePlayer, "\u00A7a\u00A7l" + it.asEntityPlayer().name + "\u00A7r is the \u00A74\u00A7lmurderer\u00A7r!")
		}
	}

	@EventTarget
	fun onWorldChange(@Suppress("UNUSED_PARAMETER") event: WorldEvent?)
	{
		murders.clear()
	}

	override val tag: String?
		get() = if (murders.isEmpty()) null else murders[0].name.toString()

	companion object
	{
		fun isMurder(item: IItem): Boolean = !classProvider.isItemMap(item) && !classProvider.isItemBow(item) && arrayOf("item.ingotGold", "item.arrow", "item.potion", "item.paper", "tile.tnt", "item.web", "item.bed", "item.compass", "item.comparator", "item.shovelWood").none { item.unlocalizedName.equals(it, ignoreCase = true) }
	}
}
