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
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.ceilToInt
import net.ccbluex.liquidbounce.utils.world.nextLocalEntityId
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntitySpawnRequest
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.max
import kotlin.math.sqrt

private const val ENTITY_TILE_SIZE = 96

private class EntityAtlas(
    val images: Map<Identifier, ByteArray>,
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

    val supportedEntityIds: Set<Identifier>
        get() = atlas?.images?.keys ?: emptySet()

    fun getEntityImage(name: Identifier): AtlasLookup {
        val atlas = this.atlas ?: return AtlasLookup.NotReady
        val bytes = atlas.images[name]
        return if (bytes == null) AtlasLookup.Missing else AtlasLookup.Found(bytes)
    }
}

private class EntityTextureRenderer : AbstractAtlasRenderer<EntityAtlas>("Entities") {

    private val entities = BuiltInRegistries.ENTITY_TYPE.mapNotNull { type ->
        val identifier = BuiltInRegistries.ENTITY_TYPE.getKey(type)
        try {
            identifier to (createEntity(type) ?: return@mapNotNull null)
        } catch (e: Exception) {
            logger.warn(
                "Unable to create entity preview for $identifier",
                e,
            )
            null
        }
    }
    override val tileSize = ENTITY_TILE_SIZE
    override val tilesPerRow = sqrt(entities.size.toDouble()).ceilToInt()

    override fun render(): CompletableFuture<EntityAtlas> = try {
        val entityMap = withAtlasTarget {
            buildMap(entities.size) {
                entities.forEachIndexed { index, (identifier, entity) ->
                    val rect = tileRect(index)
                    this[identifier] = rect

                    try {
                        renderEntity(entity, rect)
                    } catch (t: Throwable) {
                        logger.warn(
                            "Unable to render entity preview for $identifier",
                            t,
                        )
                    }
                }
            }
        }

        return readbackAsync { atlasPixels, result ->
            val atlas = EntityAtlas(encodePngTiles(atlasPixels, entityMap, result))
            logger.info("Loaded $textureSize x $textureSize entity atlas with ${atlas.images.size} PNGs")
            atlas
        }
    } catch (throwable: Throwable) {
        close()
        CompletableFuture.failedFuture(throwable)
    }

    private fun renderEntity(entity: Entity, rect: Rect2i) {
        entity.yRot = 25F
        entity.yHeadRot = 25F
        if (entity is LivingEntity) {
            entity.yBodyRot = 25F
        }

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

    private fun createEntity(type: EntityType<*>): Entity? {
        val level = requireNotNull(mc.level)
        val entity = if (type === EntityTypes.PLAYER) {
            val profile = GameProfile(
                UUID.nameUUIDFromBytes("$CLIENT_NAME Preview".toByteArray()),
                "Player",
            )
            RemotePlayer(level, profile)
        } else {
            type.create(level, EntitySpawnRequest(EntitySpawnReason.COMMAND, true))
        }

        entity?.id = level.nextLocalEntityId()
        return entity
    }

}
