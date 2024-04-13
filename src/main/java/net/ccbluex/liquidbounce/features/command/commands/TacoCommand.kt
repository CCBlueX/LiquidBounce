/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.event.EventManager.registerListener
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation

object TacoCommand : Command("taco"), Listenable {
    private var toggle = false
    private var image = 0
    private var running = 0f
    private val tacoTextures = arrayOf(
        ResourceLocation("liquidbounce/taco/1.png"),
        ResourceLocation("liquidbounce/taco/2.png"),
        ResourceLocation("liquidbounce/taco/3.png"),
        ResourceLocation("liquidbounce/taco/4.png"),
        ResourceLocation("liquidbounce/taco/5.png"),
        ResourceLocation("liquidbounce/taco/6.png"),
        ResourceLocation("liquidbounce/taco/7.png"),
        ResourceLocation("liquidbounce/taco/8.png"),
        ResourceLocation("liquidbounce/taco/9.png"),
        ResourceLocation("liquidbounce/taco/10.png"),
        ResourceLocation("liquidbounce/taco/11.png"),
        ResourceLocation("liquidbounce/taco/12.png")
    )

    init {
        registerListener(this)
    }

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        toggle = !toggle
        displayChatMessage(if (toggle) "§aTACO TACO TACO. :)" else "§cYou made the little taco sad! :(")
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!toggle)
            return

        running += 0.15f * deltaTime
        val (width, height) = ScaledResolution(mc)
        drawImage(tacoTextures[image], running.toInt(), height - 60, 64, 32)
        if (width <= running)
            running = -64f
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!toggle) {
            image = 0
            return
        }

        image++
        if (image >= tacoTextures.size) image = 0
    }

    override fun handleEvents() = true

    override fun tabComplete(args: Array<String>) = listOf("TACO")
}