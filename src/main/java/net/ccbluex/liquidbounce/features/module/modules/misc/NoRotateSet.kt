/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationSettings
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.entity.player.EntityPlayer

object NoRotateSet : Module("NoRotateSet", Category.MISC, gameDetecting = false, hideModule = false) {
    var savedRotation = Rotation.ZERO

    private val ignoreOnSpawn by BoolValue("IgnoreOnSpawn", false)
    val affectRotation by BoolValue("AffectRotation", true)

    private val options = RotationSettings(this) { affectRotation }.apply {
        rotationModeValue.set("On")
        rotationModeValue.isSupported = { false }
        applyServerSideValue.isSupported = { false }
        applyServerSideValue.note = BoolValue.NoteType.HIDE
        resetTicksValue.isSupported = { false }

        withoutKeepRotation()
    }

    fun shouldModify(player: EntityPlayer) = handleEvents() && (!ignoreOnSpawn || player.ticksExisted != 0)

    fun rotateBackToPlayerRotation() {
        val player = mc.thePlayer ?: return

        currentRotation = player.rotation

        setTargetRotation(savedRotation, options = options)
    }
}