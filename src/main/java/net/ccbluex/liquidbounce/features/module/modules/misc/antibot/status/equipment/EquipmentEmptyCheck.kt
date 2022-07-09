package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.status.equipment

import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.BotCheck

class EquipmentEmptyCheck : BotCheck("status.equipment.empty")
{
    override val isActive: Boolean
        get() = AntiBot.equipmentValue.get()

    override fun isBot(theWorld: WorldClient, thePlayer: Entity, target: EntityPlayer): Boolean
    {
        val armorInventory = target.inventory.armorInventory
        return AntiBot.equipmentBootsValue.get() && armorInventory[0] == null || AntiBot.equipmentLeggingsValue.get() && armorInventory[1] == null || AntiBot.equipmentChestplateValue.get() && armorInventory[2] == null || AntiBot.equipmentHelmetValue.get() && armorInventory[3] == null
    }
}
