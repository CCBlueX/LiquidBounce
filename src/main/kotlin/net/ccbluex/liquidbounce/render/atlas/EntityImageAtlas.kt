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

package net.ccbluex.liquidbounce.render.atlas

import com.mojang.authlib.GameProfile
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.ceilToInt
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import kotlin.math.max
import kotlin.math.sqrt

private const val ENTITY_TILE_SIZE = 96

private class EntityAtlas(
    val map: Map<EntityType<*>, Rect2i>,
    val image: BufferedImage,
)

object EntityImageAtlas : EventListener {

    @Volatile
    private var atlas: EntityAtlas? = null

    @Suppress("unused")
    private val resourceReloadHandler = suspendHandler<ResourceReloadEvent>(
        behavior = SuspendHandlerBehavior.CancelPrevious,
    ) {
        tickUntil { inGame }
        atlas = EntityTextureRenderer().render().await()
    }

    val isAtlasAvailable: Boolean
        get() = atlas != null

    val supportedEntityTypes: Set<EntityType<*>>
        get() = atlas?.map?.keys ?: emptySet()

    fun getEntityImage(type: EntityType<*>): BufferedImage? {
        val currentAtlas = requireNotNull(atlas) { "Entity atlas is not available yet" }
        val rect = currentAtlas.map[type] ?: return null

        return currentAtlas.image.getSubimage(rect.x, rect.y, rect.width, rect.height)
    }
}

private class EntityTextureRenderer : AbstractAtlasRenderer<EntityAtlas>("Entities") {

    private val entities = BuiltInRegistries.ENTITY_TYPE.mapNotNull { type ->
        runCatching { type to createLivingEntity(type) }
            .onFailure {
                logger.warn(
                    "Unable to create entity preview for ${BuiltInRegistries.ENTITY_TYPE.getKey(type)}",
                    it,
                )
            }
            .getOrNull()
            ?.takeIf { it.second != null }
            ?.let { it.first to requireNotNull(it.second) }
    }
    override val tileSize = ENTITY_TILE_SIZE
    override val tilesPerRow = sqrt(entities.size.toDouble()).ceilToInt()

    override fun render(): CompletableFuture<EntityAtlas> {
        val entityMap = Reference2ObjectOpenHashMap<EntityType<*>, Rect2i>(entities.size)
        val failedRects = mutableListOf<Rect2i>()

        withAtlasTarget {
            entities.forEachIndexed { index, (type, entity) ->
                val rect = tileRect(index)
                entityMap[type] = rect

                runCatching { renderEntity(entity, rect) }
                    .onFailure {
                        failedRects += rect
                        logger.warn(
                            "Unable to render entity preview for ${BuiltInRegistries.ENTITY_TYPE.getKey(type)}",
                            it,
                        )
                    }
            }
        }

        return framebuffer.colorTexture!!.toBufferedImage()
            .thenApply { image ->
                drawFallbacks(image, failedRects)
                logger.info("Loaded ${image.width} x ${image.height} entity atlas")
                EntityAtlas(entityMap, image)
            }.whenComplete { _, throwable ->
                close()
                if (throwable != null && throwable !is CancellationException) {
                    logger.error("Failed to load entity atlas", throwable)
                }
            }
    }

    private fun renderEntity(entity: LivingEntity, rect: Rect2i) {
        entity.yRot = 25F
        entity.yHeadRot = 25F
        entity.yBodyRot = 25F

        val state = mc.entityRenderDispatcher.extractEntity(entity, 1F)
        state.nameTag = null
        state.shadowPieces.clear()
        state.outlineColor = 0
        val scale = rect.height * 0.72F / max(entity.bbHeight, entity.bbWidth * 1.5F)

        withTile(rect) {
            translate(
                rect.x + rect.width * 0.5F,
                rect.y + rect.height * 0.88F,
                0F,
            )
            scale(scale, -scale, scale)

            val cameraState = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
            mc.entityRenderDispatcher.submit(state, cameraState, 0.0, 0.0, 0.0, this, submitNodeStorage)
            featureRenderDispatcher.renderAllFeatures(submitNodeStorage)
        }
    }

    private fun createLivingEntity(type: EntityType<*>): LivingEntity? {
        val level = requireNotNull(mc.level)
        val entity = if (type === EntityTypes.PLAYER) {
            val profile = GameProfile(
                UUID.nameUUIDFromBytes("LiquidBounce Preview".toByteArray()),
                "Player",
            )
            RemotePlayer(level, profile)
        } else {
            type.create(level, EntitySpawnReason.COMMAND) as? LivingEntity
        }

        entity?.id = level.nextLocalEntityId()
        return entity
    }

    private fun drawFallbacks(image: BufferedImage, rects: Collection<Rect2i>) {
        if (rects.isEmpty()) {
            return
        }

        val graphics = image.createGraphics()
        try {
            graphics.color = Color(70, 70, 70, 180)
            rects.forEach { rect ->
                val inset = ENTITY_TILE_SIZE / 4
                graphics.fillOval(
                    rect.x + inset,
                    rect.y + inset,
                    ENTITY_TILE_SIZE - inset * 2,
                    ENTITY_TILE_SIZE - inset * 2,
                )
            }
            graphics.color = Color.WHITE
            graphics.font = graphics.font.deriveFont(ENTITY_TILE_SIZE * 0.35F)
            rects.forEach { rect ->
                val width = graphics.fontMetrics.stringWidth("?")
                graphics.drawString("?", rect.x + (ENTITY_TILE_SIZE - width) / 2, rect.y + ENTITY_TILE_SIZE * 2 / 3)
            }
        } finally {
            graphics.dispose()
        }
    }
}
