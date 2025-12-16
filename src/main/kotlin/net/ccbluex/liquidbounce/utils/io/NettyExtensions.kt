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
package net.ccbluex.liquidbounce.utils.io

import io.netty.bootstrap.AbstractBootstrap
import io.netty.channel.Channel
import net.minecraft.network.NetworkingBackend

/**
 * Shortcut for Netty client [io.netty.bootstrap.Bootstrap],
 * using shared [io.netty.channel.EventLoopGroup] from [NetworkingBackend]
 */
internal fun <B : AbstractBootstrap<B, Channel>> AbstractBootstrap<B, Channel>.clientChannelAndGroup(
    useEpoll: Boolean = true
): B {
    val networkingBackend = NetworkingBackend.remote(useEpoll)
    return channel(networkingBackend.channelClass)
            .group(networkingBackend.eventLoopGroup)
}

