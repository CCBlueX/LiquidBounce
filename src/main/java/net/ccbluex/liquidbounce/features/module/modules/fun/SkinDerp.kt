/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.player.EnumPlayerModelParts
import kotlin.random.Random.Default.nextBoolean

object SkinDerp : Module("SkinDerp", ModuleCategory.FUN) {

    private val delay by IntegerValue("Delay", 0, 0..1000)
    private val hat by BoolValue("Hat", true)
    private val jacket by BoolValue("Jacket", true)
    private val leftPants by BoolValue("LeftPants", true)
    private val rightPants by BoolValue("RightPants", true)
    private val leftSleeve by BoolValue("LeftSleeve", true)
    private val rightSleeve by BoolValue("RightSleeve", true)

    private var prevModelParts = emptySet<EnumPlayerModelParts>()

    private val timer = MSTimer()

    override fun onEnable() {
        prevModelParts = mc.gameSettings.modelParts

        super.onEnable()
    }

    override fun onDisable() {
        // Disable all current model parts

        for (modelPart in mc.gameSettings.modelParts)
            mc.gameSettings.setModelPartEnabled(modelPart, false)

        // Enable all old model parts
        for (modelPart in prevModelParts)
            mc.gameSettings.setModelPartEnabled(modelPart, true)

        super.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (timer.hasTimePassed(delay)) {
            if (hat)
                mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.HAT, nextBoolean())
            if (jacket)
                mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.JACKET, nextBoolean())
            if (leftPants)
                mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.LEFT_PANTS_LEG, nextBoolean())
            if (rightPants)
                mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.RIGHT_PANTS_LEG, nextBoolean())
            if (leftSleeve)
                mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.LEFT_SLEEVE, nextBoolean())
            if (rightSleeve)
                mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.RIGHT_SLEEVE, nextBoolean())
            timer.reset()
        }
    }

}
