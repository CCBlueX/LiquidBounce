/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.entity.player.EntityPlayer

object NoRotateSet : Module("NoRotateSet", ModuleCategory.MISC, gameDetecting = false, hideModule = false) {
    var savedRotation = Rotation(0f, 0f)

    private val ignoreOnSpawn by BoolValue("IgnoreOnSpawn", false)
    val affectServerRotation by BoolValue("AffectServerRotation", true)

    fun shouldModify(player: EntityPlayer) = handleEvents() && (!ignoreOnSpawn || player.ticksExisted != 0)
}