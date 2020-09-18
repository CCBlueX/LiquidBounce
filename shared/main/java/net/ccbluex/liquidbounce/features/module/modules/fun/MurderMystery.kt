package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.item.ItemSword

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
        for (entity in mc.theWorld!!.loadedEntityList) {
            if(!classProvider.isEntityLivingBase(entity) || AntiBot.isBot(entity.asEntityLivingBase())) continue
//            isMurderer(entity)
        }
    }

//    fun isMurderer(entity: EntityLivingBase) {
//        val i = 1
//        for(i in 0..9) {
//            if(entity.heldItem.item is ItemSword && murderer != entity.name) {
//                murderer = entity.name
//                ClientUtils.displayChatMessage("murderer is ${murderer}")
//            }
//        }
//    }
    fun isMurderer(entity: Entity) {
        for(i in 0..9) {
            if(entity.inventory.get(i).item is ItemSword && murderer != entity.name) {
                murderer = entity.name
                reason = "Holding a sword"
                ClientUtils.displayChatMessage("The murderer is $murderer. $reason")
            }
        }
    }

    override fun onEnable() {
        ClientUtils.displayChatMessage("Searching for murderer")
    }

}