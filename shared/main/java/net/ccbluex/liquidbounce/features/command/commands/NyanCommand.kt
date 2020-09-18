package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class NyanCommand : Command("nyan"), Listenable {
    private var toggle = false
    private var image = 0
    private var running = 0f
    private val nyanTextures = arrayOf(
            classProvider.createResourceLocation("liquidbounce/nyan/1.png"),
            classProvider.createResourceLocation("liquidbounce/nyan/2.png"),
            classProvider.createResourceLocation("liquidbounce/nyan/3.png"),
            classProvider.createResourceLocation("liquidbounce/nyan/4.png"),
            classProvider.createResourceLocation("liquidbounce/nyan/5.png")
    )

    init {
        LiquidBounce.eventManager.registerListener(this)
    }

    override fun execute(args: Array<String>) {
        toggle = !toggle
        ClientUtils.displayChatMessage(if (toggle) "§anyan nyan nyan. :)" else "§cYou made the little nyan sad! :(")
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!toggle)
            return

        running += 0.15f * RenderUtils.deltaTime
        val scaledResolution = classProvider.createScaledResolution(mc)
        RenderUtils.drawImage(nyanTextures[image], running.toInt(), scaledResolution.scaledHeight - 60, 64, 32)
        if (scaledResolution.scaledWidth <= running)
            running = -64f
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!toggle) {
            image = 0
            return
        }

        image++
        if (image >= nyanTextures.size) image = 0
    }

    override fun handleEvents() = true

    override fun tabComplete(args: Array<String>): List<String> {
        return listOf("nyan")
    }
}