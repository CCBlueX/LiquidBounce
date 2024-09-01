/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.render.entity.PlayerModelPart
import kotlin.random.Random.Default.nextBoolean

object SkinDerp : Module("SkinDerp", Category.FUN, subjective = true, hideModule = false) {

    private val delay by IntegerValue("Delay", 0, 0..1000)
    private val hat by BoolValue("Hat", true)
    private val jacket by BoolValue("Jacket", true)
    private val leftPants by BoolValue("LeftPants", true)
    private val rightPants by BoolValue("RightPants", true)
    private val leftSleeve by BoolValue("LeftSleeve", true)
    private val rightSleeve by BoolValue("RightSleeve", true)

    private var prevModelParts = emptySet<PlayerModelPart>()

    private val timer = MSTimer()

    override fun onEnable() {
        prevModelParts = mc.options.enabledPlayerModelParts

        super.onEnable()
    }

    override fun onDisable() {
        // Disable all current model parts

        for (modelPart in mc.options.enabledPlayerModelParts)
            mc.options.setPlayerModelPart(modelPart, false)

        // Enable all old model parts
        for (modelPart in prevModelParts)
            mc.options.setPlayerModelPart(modelPart, true)

        super.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (timer.hasTimePassed(delay)) {
            if (hat)
                mc.options.setPlayerModelPart(PlayerModelPart.HAT, nextBoolean())
            if (jacket)
                mc.options.setPlayerModelPart(PlayerModelPart.JACKET, nextBoolean())
            if (leftPants)
                mc.options.setPlayerModelPart(PlayerModelPart.LEFT_PANTS_LEG, nextBoolean())
            if (rightPants)
                mc.options.setPlayerModelPart(PlayerModelPart.RIGHT_PANTS_LEG, nextBoolean())
            if (leftSleeve)
                mc.options.setPlayerModelPart(PlayerModelPart.LEFT_SLEEVE, nextBoolean())
            if (rightSleeve)
                mc.options.setPlayerModelPart(PlayerModelPart.RIGHT_SLEEVE, nextBoolean())
            timer.reset()
        }
    }

}
