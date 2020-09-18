package net.ccbluex.liquidbounce.features.module.modules.`fun`

import com.sun.security.ntlm.Client
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraft.entity.Entity
import net.minecraft.item.ItemSword

@ModuleInfo(name = "MurderMystery", description = "Tells you who the murderer is", category = ModuleCategory.FUN)
class MurderMystery : Module() {

    private val toolValue = BoolValue("Tools", true)
    private val miscValue = BoolValue("Misc", true)

    // globals
    private var murderer = ""
    private val customMessage = "The murderer is ${murderer} lol"

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        for (entity in mc.theWorld!!.loadedEntityList) {
            if(!classProvider.isEntityLivingBase(entity)) continue
//            isMurderer(entity)
        }
        ClientUtils.displayChatMessage("Checking for murderer...")
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
                ClientUtils.displayChatMessage("Murderer is $murderer")
            }
        }
    }

}