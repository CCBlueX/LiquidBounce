/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.render.totemeffect

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.phys.Vec3
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.modes.TotemEffectShockwave
import net.ccbluex.liquidbounce.features.module.modules.render.totemeffect.modes.TotemEffectSoul
import net.ccbluex.liquidbounce.utils.network.isDeathProtection

object ModuleTotemEffect : ClientModule("TotemEffect", ModuleCategories.RENDER) {

    val coords = ExpiringList<Vec3>()

    val modes = choices("Mode", 0) {
        arrayOf(
            TotemEffectShockwave,
            TotemEffectSoul
        )
    }.apply { tagBy(this); onChanged { coords.clear() } }

    override fun onDisabled() = coords.clear()

    @Suppress("unused")
    private val totemHandler = handler<PacketEvent> { event ->
        val packet = event.packet as? ClientboundEntityEventPacket ?: return@handler
        if (!packet.isDeathProtection) return@handler

        mc.execute {
            val entity = event.packet.getEntity(world) ?: return@execute

            val lifetime = modes.activeMode.lifetime
            coords.add(entity.position().add(0.0, entity.bbHeight / 2.0, 0.0), lifetime)
        }
    }

}
