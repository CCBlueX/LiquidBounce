/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.render.Animations.animations
import net.ccbluex.liquidbounce.features.module.modules.render.Animations.defaultAnimation
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import org.lwjgl.opengl.GL11

/**
 * Animations module
 *
 * This module affects the blocking animation. It allows the user to customize the animation.
 * If you are looking forward to contribute to this module, please name your animation with a reasonable name. Do not name them after clients or yourself.
 * Please credit from where you got the animation from and make sure they are willing to contribute.
 * If they are not willing to contribute, please do not add the animation to this module.
 *
 * If you are looking for the animation classes, please look at the [Animation] class. It allows you to create your own animation.
 * After making your animation class, please add it to the [animations] array. It should automatically be added to the list and show up in the GUI.
 *
 * By default, the module uses the [OneSevenAnimation] animation. If you want to change the default animation, please change the [defaultAnimation] variable.
 * Default animations are even used when the module is disabled.
 *
 * If another variables from the renderItemInFirstPerson method are needed, please let me know or pass them by yourself.
 *
 * @author CCBlueX
 */
@ModuleInfo(name = "Animations", description = "Customizes your blocking animation.", category = ModuleCategory.RENDER)
object Animations : Module() {

    // Default animation
    val defaultAnimation = OneSevenAnimation()

    private val animations = arrayOf(
        OneSevenAnimation(),
        PushdownAnimation()
    )

    private var animationMode = ListValue("Mode", animations.map { it.name }.toTypedArray(), "Pushdown")
    var oddSwing = BoolValue("OddSwing", false)

    fun getAnimation() = animations.firstOrNull { it.name.equals(animationMode.get(), true) }

}

/**
 * Sword Animation
 *
 * This class allows you to create your own animation.
 * It transforms the item in the hand and the known functions from Mojang are directly accessible as well.
 *
 * @author CCBlueX
 */
abstract class Animation(val name: String) : MinecraftInstance() {
    abstract fun transform(f1: Float, f: Float, clientPlayer: AbstractClientPlayer)

    /**
     * Transforms the block in the hand
     *
     * @author Mojang
     */
    protected fun doBlockTransformations() {
        GlStateManager.translate(-0.5f, 0.2f, 0.0f)
        GlStateManager.rotate(30.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(-80.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(60.0f, 0.0f, 1.0f, 0.0f)
    }

    /**
     * Transforms the item in the hand
     *
     * @author Mojang
     */
    protected fun transformFirstPersonItem(equipProgress: Float, swingProgress: Float) {
        GlStateManager.translate(0.56f, -0.52f, -0.71999997f)
        GlStateManager.translate(0.0f, equipProgress * -0.6f, 0.0f)
        GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f)
        val f = MathHelper.sin(swingProgress * swingProgress * 3.1415927f)
        val f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * 3.1415927f)
        GlStateManager.rotate(f * -20.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(f1 * -20.0f, 0.0f, 0.0f, 1.0f)
        GlStateManager.rotate(f1 * -80.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(0.4f, 0.4f, 0.4f)
    }

}

/**
 * OneSeven animation (default). Similar to the 1.7 blocking animation.
 *
 * @author CCBlueX
 */
class OneSevenAnimation : Animation("OneSeven") {
    override fun transform(f1: Float, f: Float, clientPlayer: AbstractClientPlayer) {
        transformFirstPersonItem(f + 0.1f, f1)
        doBlockTransformations()
        GlStateManager.translate(-0.5f, 0.2f, 0.0f)
    }

}

/**
 * Pushdown animation
 */
class PushdownAnimation : Animation("Pushdown") {

    /**
     * @author CzechHek. Taken from Animations script.
     */
    override fun transform(f1: Float, f: Float, clientPlayer: AbstractClientPlayer) {
        GlStateManager.translate(0.56, -0.52, -0.5)
        GlStateManager.translate(0.0, f * -0.0, 0.0)
        GlStateManager.rotate(45.5f, 0.0f, 1.0f, 0.0f)
        val var3 = MathHelper.sin(0f)
        val var4 = MathHelper.sin(0f)
        GlStateManager.rotate((var3 * -20.0f), 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate((var4 * -20.0f), 0.0f, 0.0f, 1.0f)
        GlStateManager.rotate((var4 * -80.0f), 1.0f, 0.0f, 0.0f)
        GlStateManager.scale(0.32, 0.32, 0.32)
        val var15 = MathHelper.sin((MathHelper.sqrt_float(f1) * 3.1415927f))
        GlStateManager.rotate((-var15 * 125 / 1.75f), 3.95f, 0.35f, 8.0f)
        GlStateManager.rotate(-var15 * 35, 0.0f, (var15 / 100.0f), -10.0f)
        GlStateManager.translate(-1.0, 0.6, -0.0)
        GlStateManager.rotate(30.0f, 0.0f, 1.0f, 0.0f)
        GlStateManager.rotate(-80.0f, 1.0f, 0.0f, 0.0f)
        GlStateManager.rotate(60.0f, 0.0f, 1.0f, 0.0f)
        GL11.glTranslated(1.05, 0.35, 0.4)
        GL11.glTranslatef(-1f, (if (clientPlayer.isSneaking) 0.0f else 0.0f), 0f)
    }

}