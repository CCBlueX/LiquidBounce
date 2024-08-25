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
import net.minecraft.client.util.Window
import net.minecraft.util.Identifier

object TacoCommand : Command("taco"), Listenable {
    var tacoToggle = false
    private var image = 0
    private var running = 0f
    private val tacoTextures = arrayOf(
        Identifier("liquidbounce/taco/1.png"),
        Identifier("liquidbounce/taco/2.png"),
        Identifier("liquidbounce/taco/3.png"),
        Identifier("liquidbounce/taco/4.png"),
        Identifier("liquidbounce/taco/5.png"),
        Identifier("liquidbounce/taco/6.png"),
        Identifier("liquidbounce/taco/7.png"),
        Identifier("liquidbounce/taco/8.png"),
        Identifier("liquidbounce/taco/9.png"),
        Identifier("liquidbounce/taco/10.png"),
        Identifier("liquidbounce/taco/11.png"),
        Identifier("liquidbounce/taco/12.png")
    )

    init {
        registerListener(this)
    }

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        tacoToggle = !tacoToggle
        displayChatMessage(if (tacoToggle) "§aTACO TACO TACO. :)" else "§cYou made the little taco sad! :(")
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!tacoToggle)
            return

        running += 0.15f * deltaTime
        val (width, height) = Window(mc)
        drawImage(tacoTextures[image], running.toInt(), height - 60, 64, 32)
        if (width <= running)
            running = -64f
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!tacoToggle) {
            image = 0
            return
        }

        image++
        if (image >= tacoTextures.size) image = 0
    }

    override fun handleEvents() = true

    override fun tabComplete(args: Array<String>) = listOf("TACO")
}