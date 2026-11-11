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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.minecraft.client.renderer.extract.LevelExtractor
import java.util.concurrent.ConcurrentHashMap

/**
 * Live views of the background clients, drawn while someone watches them.
 */
object ClientViews {

    private const val FRAME_INTERVAL_NANOS = 100_000_000L

    /**
     * A view outlives its last stream a little, so that a page reload does not compile the chunks again.
     */
    private const val IDLE_NANOS = 5_000_000_000L

    private val frames = ConcurrentHashMap<Client, ClientViewFrames>()

    // Render thread only
    private val views = Reference2ObjectOpenHashMap<Client, ClientView>()
    private val extractors = ReferenceOpenHashSet<LevelExtractor>()

    /**
     * Runs [block] with the JPEG frames of what [client] sees. Frames are drawn as long as a block runs.
     */
    suspend fun <T> watch(client: Client, block: suspend (Flow<ByteArray>) -> T): T {
        val frames = frames.compute(client) { _, frames ->
            (frames ?: ClientViewFrames()).also { it.watchers++ }
        }!!

        try {
            return block(frames.latest.filterNotNull().takeWhile { it !== ClientViewFrames.END })
        } finally {
            this.frames.computeIfPresent(client) { _, frames ->
                frames.watchers--
                frames.leftAt = System.nanoTime()
                frames
            }
        }
    }

    /**
     * Draws the view of [client] if it is watched and due. Runs within [client].
     */
    internal fun render(client: Client) {
        val frames = frames.computeIfPresent(client) { _, frames ->
            frames.takeIf { it.watchers > 0 || System.nanoTime() - it.leftAt < IDLE_NANOS }
        }
        if (frames == null) {
            close(client)
            return
        }

        if (frames.watchers == 0) {
            return
        }

        val view = views.getOrPut(client) { ClientView(client, frames) }
        if (System.nanoTime() - view.renderedAt >= FRAME_INTERVAL_NANOS) {
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
     * Ends the streams of [client], which is gone.
     */
    internal fun forget(client: Client) {
        close(client)
        frames.remove(client)?.latest?.value = ClientViewFrames.END
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
