/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.RotationUpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationSettings
import net.ccbluex.liquidbounce.utils.RotationUtils.faceTrajectory
import net.ccbluex.liquidbounce.utils.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemEgg
import net.minecraft.item.ItemEnderPearl
import net.minecraft.item.ItemSnowball
import java.awt.Color

object ProjectileAimbot : Module("ProjectileAimbot", Category.COMBAT, hideModule = false) {

    private val bow by BoolValue("Bow", true, subjective = true)
    private val egg by BoolValue("Egg", true, subjective = true)
    private val snowball by BoolValue("Snowball", true, subjective = true)
    private val pearl by BoolValue("EnderPearl", false, subjective = true)

    private val priority by ListValue("Priority",
        arrayOf("Health", "Distance", "Direction"),
        "Direction",
        subjective = true
    )

    private val predict by BoolValue("Predict", true)
    private val predictSize by FloatValue("PredictSize", 2F, 0.1F..5F) { predict }

    private val throughWalls by BoolValue("ThroughWalls", false, subjective = true)
    private val mark by BoolValue("Mark", true, subjective = true)

    private val options = RotationSettings(this).withoutKeepRotation().apply {
        rotationModeValue.set("On")
        rotationModeValue.isSupported = { false }
    }

    private var target: Entity? = null

    override fun onDisable() {
        target = null
    }

    @EventTarget
    fun onRotationUpdate(event: RotationUpdateEvent) {
        target = null

        val targetRotation = when (val item = mc.thePlayer.heldItem?.item) {
            is ItemBow -> {
                if (!bow || !mc.thePlayer.isUsingItem)
                    return

                target = getTarget(throughWalls, priority)

                faceTrajectory(target ?: return, predict, predictSize)
            }

            is ItemEgg, is ItemSnowball, is ItemEnderPearl -> {
                if (!egg && item is ItemEgg || !snowball && item is ItemSnowball || !pearl && item is ItemEnderPearl)
                    return

                target = getTarget(throughWalls, priority)

                faceTrajectory(target ?: return, predict, predictSize, gravity = 0.03f, velocity = 0.5f)
            }

            else -> return
        }

        setTargetRotation(targetRotation, options = options)
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (target != null && priority != "Multi" && mark) {
            drawPlatform(target!!, Color(37, 126, 255, 70))
        }
    }

    private fun getTarget(throughWalls: Boolean, priorityMode: String): Entity? {
        val targets = mc.theWorld.loadedEntityList.filter {
            it is EntityLivingBase && isSelected(it, true) && (throughWalls || mc.thePlayer.canEntityBeSeen(it))
        }

        return when (priorityMode.uppercase()) {
            "DISTANCE" -> targets.minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) }
            "DIRECTION" -> targets.minByOrNull { rotationDifference(it) }
            "HEALTH" -> targets.minByOrNull { (it as EntityLivingBase).health }
            else -> null
        }
    }

    fun hasTarget() = target != null && mc.thePlayer.canEntityBeSeen(target)
}
