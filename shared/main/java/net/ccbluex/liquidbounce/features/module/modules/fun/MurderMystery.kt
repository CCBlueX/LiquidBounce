package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.item.ItemSword
import java.lang.Exception

@ModuleInfo(name = "MurderMystery", description = "Tells you who the murderer is", category = ModuleCategory.FUN)
class MurderMystery : Module() {

    private val toolValue = BoolValue("Tools", true)
    private val miscValue = BoolValue("Misc", true)
    private val modeValue = ListValue("Mode", arrayOf("Single", "Double"), "Single")

    // globals
    private var murderer = ""
    private var reason = "error"

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        for (entity in mc.theWorld!!.playerEntities) {
            if(AntiBot.isBot(entity)) continue
            isMurderer(entity)
        }
    }

    fun isMurderer(entity: IEntity) {
        try {
            if(entity.asEntityPlayer().heldItem!!.item is ItemSword && murderer != entity.name) {
                murderer = entity.name.toString()
                reason = "Holding a sword"
                ClientUtils.displayChatMessage("The murderer is $murderer. $reason")
            }
        }
        catch(e : Exception) {
            ClientUtils.displayChatMessage("oops there was a fw")
            ClientUtils.displayChatMessage(e.message)
            MurderMystery().state = false
        }

    }

    override fun onEnable() {
        ClientUtils.displayChatMessage("Searching for murderer")
    }

    override fun onDisable() {
        ClientUtils.displayChatMessage("kk good luck ♥")
    }

}