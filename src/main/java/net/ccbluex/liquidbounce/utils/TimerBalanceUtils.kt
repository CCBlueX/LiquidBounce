/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object TimerBalanceUtils : MinecraftInstance(), Listenable {

    private var balance = 0L
    private var frametime = -1L
    private var prevframetime = -1L
    private var currframetime = -1L

    private val inGame: Boolean
        get() = mc.player != null && mc.world != null && mc.networkHandler != null && mc.interactionManager != null

    @EventTarget
    fun onGameLoop(event: GameLoopEvent) {
        if (frametime == -1L) {
            frametime = 0L
            currframetime = System.currentTimeMillis()
            prevframetime = currframetime
        }

        prevframetime = currframetime
        currframetime = System.currentTimeMillis()
        frametime = currframetime - prevframetime

        if (inGame) {
            balance -= frametime
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (inGame) {
            if (packet is PlayerMoveC2SPacket) {
                balance += 50
            }
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        balance = 0
    }

    fun getBalance(): Long {
        return balance
    }

    override fun handleEvents() = true
}