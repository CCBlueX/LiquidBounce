/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import java.util.concurrent.CopyOnWriteArrayList

object TickedActions : Listenable {
    private val actions = CopyOnWriteArrayList<Triple<Module, Int, () -> Unit>>()

    private val calledThisTick = mutableListOf<Triple<Module, Int, () -> Unit>>()

    fun schedule(id: Int, module: Module, allowDuplicates: Boolean = false, action: () -> Unit) =
        if (allowDuplicates || !isScheduled(id, module)) {
            actions += Triple(module, id, action)
            true
        } else false

    fun isScheduled(id: Int, module: Module) =
        actions.filter { it.first == module && it.second == id }
            .any { it !in calledThisTick }

    fun clear(module: Module) = actions.removeIf { it.first == module }

    fun size(module: Module) = actions.count { it.first == module }

    fun isEmpty(module: Module) = size(module) == 0

    @EventTarget(priority = 1)
    fun onTick(event: TickEvent) {
        // Prevent new scheduled ids from getting marked as duplicates even if they are going to be called next tick
        actions.toCollection(calledThisTick)

        for (triple in calledThisTick) {
            triple.third()
            actions.removeFirst()
        }

        calledThisTick.clear()
    }

    @EventTarget
    fun onWorld(event: WorldEvent) = actions.clear()

    override fun handleEvents() = true
}