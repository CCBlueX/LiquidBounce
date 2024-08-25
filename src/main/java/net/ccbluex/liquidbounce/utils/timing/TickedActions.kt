/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.CoroutineUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.item.ItemStack
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
    fun onTick(event: GameTickEvent) {
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

    class TickScheduler(val module: Module) : MinecraftInstance() {
        fun schedule(id: Int, allowDuplicates: Boolean = false, action: () -> Unit) =
            schedule(id, module, allowDuplicates, action)

        fun scheduleClick(slot: Int, button: Int, mode: Int, allowDuplicates: Boolean = false, windowId: Int = mc.player.openContainer.windowId, action: ((ItemStack?) -> Unit)? = null) =
            schedule(slot, module, allowDuplicates) {
                val newStack = mc.interactionManager.windowClick(windowId, slot, button, mode, mc.player)
                action?.invoke(newStack)
            }

        operator fun plusAssign(action: () -> Unit) {
            schedule(-1, module, true, action)
        }

        // Schedule actions to be executed in following ticks, one each tick
        // Thread is frozen until all actions were executed (suitable for coroutines)
        fun scheduleAndSuspend(vararg actions: () -> Unit) =
            actions.forEach {
                this += it
                CoroutineUtils.waitUntil(::isEmpty)
            }

        fun scheduleAndSuspend(id: Int = -1, allowDuplicates: Boolean = true, action: () -> Unit) {
            schedule(id, module, allowDuplicates, action)
            CoroutineUtils.waitUntil(::isEmpty)
        }

        // Checks if id click is scheduled: if (id in TickScheduler)
        operator fun contains(id: Int) = isScheduled(id, module)

        fun clear() = clear(module)

        val size
            get() = size(module)

        fun isEmpty() = isEmpty(module)
    }
}