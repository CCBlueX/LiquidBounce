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
package net.ccbluex.liquidbounce.features.client.view

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.minecraft.client.renderer.extract.LevelExtractor
import java.util.concurrent.ConcurrentHashMap

/**
 * Live views of the background clients, drawn while someone keeps asking for them.
 */
object ClientViews {

    private const val FRAME_INTERVAL_NANOS = 100_000_000L

    /**
     * How long one request keeps a view drawn.
     */
    private const val WATCHED_NANOS = 1_000_000_000L

    /**
     * A view outlives its last request a little, so that a page reload does not compile the chunks again.
     */
    private const val IDLE_NANOS = 5_000_000_000L

    private val frames = ConcurrentHashMap<Client, ClientViewFrames>()

    // Render thread only
    private val views = Reference2ObjectOpenHashMap<Client, ClientView>()
    private val extractors = ReferenceOpenHashSet<LevelExtractor>()

    /**
     * The latest JPEG of what [client] sees, null until one is drawn. Asking keeps the view drawn for a second.
     *
     * Pages ask again and again instead of keeping a stream open: Chromium allows six connections per host,
     * and a stream for every view would leave none for the rest of the theme.
     */
    fun latestFrame(client: Client): ByteArray? {
        val frames = frames.computeIfAbsent(client) { ClientViewFrames() }
        frames.askedAt = System.nanoTime()
        return frames.latest
    }

    /**
     * Draws the watched view that waited longest, if it is due. Only one per frame: the uniforms of the renderers
     * live in rings of three buffers, which one frame must not go round, and the window takes one of them.
     */
    internal fun renderNext() {
        val now = System.nanoTime()
        var next: Client? = null
        var nextAt = Long.MAX_VALUE
        for ((client, frames) in frames) {
            val asked = now - frames.askedAt
            if (asked >= IDLE_NANOS) {
                this.frames.remove(client, frames)
                close(client)
                continue
            }
            if (asked >= WATCHED_NANOS || client.isActive) {
                continue
            }

            val renderedAt = views[client]?.renderedAt ?: 0L
            if (now - renderedAt >= FRAME_INTERVAL_NANOS && renderedAt < nextAt) {
                next = client
                nextAt = renderedAt
            }
        }

        val client = next ?: return
        val view = views.getOrPut(client) { ClientView(client, frames[client] ?: return) }
        ClientContext.within(client) {
            view.fit()
            view.extractor?.let(extractors::add)
            view.render()
        }
    }

    /**
     * Drops the view of [client], for when it takes over the window.
     */
    internal fun close(client: Client) {
        val view = views.remove(client) ?: return
        view.extractor?.let(extractors::remove)
        view.close()
    }

    /**
     * Drops the view and frames of [client], which is gone.
     */
    internal fun forget(client: Client) {
        close(client)
        frames.remove(client)
    }

    @JvmStatic
    fun closeAll() {
        views.keys.toList().forEach(::close)
    }

    @JvmStatic
    fun isView(extractor: LevelExtractor) = extractor in extractors

    /**
     * The extractor that should hear about a change reported to [extractor]. Levels of background clients report
     * to the window's, which draws the active client only, so their changes go to their view if there is one.
     */
    @JvmStatic
    fun receiverOf(extractor: LevelExtractor): LevelExtractor? =
        if (!ClientContext.isBackground || extractor in extractors) {
            extractor
        } else {
            views[ClientContext.loaded]?.extractor
        }

}
