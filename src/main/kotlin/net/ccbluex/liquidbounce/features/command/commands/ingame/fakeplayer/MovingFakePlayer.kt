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
package net.ccbluex.liquidbounce.features.command.commands.ingame.fakeplayer

import com.mojang.authlib.GameProfile
import net.minecraft.client.world.ClientWorld

/**
 * A special [FakePlayer] that moves following a recorded path
 * of [PosPoseSnapshot].
 */
class MovingFakePlayer(
    private vararg val snapshots: PosPoseSnapshot,
    clientWorld: ClientWorld?,
    gameProfile: GameProfile?,
) : FakePlayer(
    clientWorld, gameProfile,
) {

    private var index: Int = 0

    override fun tick() {
        loadAttributes(snapshots[index])
        index++
        index %= snapshots.size

        super.tick()
    }

}
