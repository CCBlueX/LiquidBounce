/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.event.EventListener
import net.minecraft.network.packet.Packet

/**
 * Options Define when the CA should run. Only tick is the most legit.
 */
abstract class Trigger(val name: String, val default: Boolean) : EventListener {

    lateinit var option: Value<Boolean>

    val enabled
        get() = option.get()

    open val allowsCaching
        get() = false

    override val running: Boolean
        get() = CrystalAuraTriggerer.running && enabled

}

abstract class PostPacketTrigger<T : Packet<*>>(name: String, default: Boolean) : Trigger(name, default) {

    fun notify(packet: T) {
        if (running) {
            postPacketHandler(packet)
        }
    }

    protected abstract fun postPacketHandler(packet: T)

}
