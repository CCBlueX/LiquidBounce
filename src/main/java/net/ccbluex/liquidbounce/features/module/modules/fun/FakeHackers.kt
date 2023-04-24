package net.ccbluex.liquidbounce.features.module.modules.fun

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer

@ModuleInfo(name = "FakeHackers", description = "Type .fakehacker <name> and record him! ;-)", category = ModuleCategory.FUN)
class FakeHackers : Module() {

    companion object {
        val fakeHackers = ArrayList<String>()

        fun isFakeHacker(player: EntityPlayer): Boolean {
            for (name in fakeHackers) {
                val en = mc.theWorld.getPlayerEntityByName(name) ?: continue
                if (player.isEntityEqual(en)) {
                    return true
                }
            }
            return false
        }

        fun removeHacker(en: EntityPlayer) {
            fakeHackers.removeIf { name -> en.isEntityEqual(mc.theWorld.getPlayerEntityByName(name)) }
            en.setSneaking(false)
        }
    }

    override fun onDisable() {
        if (state) {
            for (name in fakeHackers) {
                val player = mc.theWorld.getPlayerEntityByName(name)
                player?.setSneaking(false)
            }
        }
        super.onDisable()
    }

    override fun onEnable() {
        fakeHackers.clear()
        super.onEnable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        for (name in fakeHackers) {
            val player = mc.theWorld.getPlayerEntityByName(name) ?: continue
            if (mode.equals("KillAura", ignoreCase = true)) {
                val toFace = RotationUtils.getClosestEntityToEntity(6f, player) as? EntityLivingBase ?: continue
                val rots = RotationUtils.getFacePosEntityRemote(player, toFace)
                player.swingItem()
                player.rotationYawHead = rots[0]
                player.rotationPitch = rots[1]
            }
        }
    }

    override fun getModes(): Array<String> {
        return arrayOf("KillAura")
    }
}