package net.ccbluex.liquidbounce.render.gui

import com.mojang.authlib.GameProfile
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import kotlinx.coroutines.future.await
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
import net.ccbluex.liquidbounce.event.events.ResourceReloadEvent
import net.ccbluex.liquidbounce.event.suspendHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.collection.Pools
import net.ccbluex.liquidbounce.utils.math.ceilToInt
import net.ccbluex.liquidbounce.utils.render.clearColorAndDepth
import net.ccbluex.liquidbounce.utils.render.toBufferedImage
import net.ccbluex.liquidbounce.utils.render.withOutputTextureOverride
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.Projection
import net.minecraft.client.renderer.ProjectionMatrixBuffer
import net.minecraft.client.renderer.Rect2i
import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.LivingEntity
import org.apache.commons.lang3.function.Consumers
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import kotlin.math.max
import kotlin.math.sqrt

private const val ENTITY_TILE_SIZE = 96

private data class EntityAtlas(
    val map: Map<EntityType<*>, Rect2i>,
    val image: BufferedImage,
)

object EntityImageAtlas : EventListener {

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

private class EntityTextureRenderer : MinecraftShortcuts {

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
    private val itemsPerDimension = sqrt(entities.size.toDouble()).ceilToInt()
    private val textureSize = ENTITY_TILE_SIZE * itemsPerDimension
    private val framebuffer = TextureTarget(
        "EntityImageAtlas Framebuffer",
        textureSize,
        textureSize,
        true,
        GpuFormat.RGBA8_UNORM,
    )
    private val submitNodeCollector = SubmitNodeStorage()
    private val featureRenderDispatcher = FeatureRenderDispatcher(
        mc.gameRenderer.renderBuffers,
        mc.modelManager,
        mc.atlasManager,
        mc.font,
        mc.gameRenderer.gameRenderState(),
    )
    private val projection = Projection()
    private val projectionMatrixBuffer = ProjectionMatrixBuffer("entities")

    fun render(): CompletableFuture<EntityAtlas> {
        framebuffer.clearColorAndDepth()
        RenderSystem.backupProjectionMatrix()
        projection.setupOrtho(-1000.0F, 1000.0F, textureSize.toFloat(), textureSize.toFloat(), true)
        RenderSystem.setProjectionMatrix(projectionMatrixBuffer.getBuffer(projection), ProjectionType.ORTHOGRAPHIC)

        val entityMap = Reference2ObjectOpenHashMap<EntityType<*>, Rect2i>(entities.size)
        val failedRects = mutableListOf<Rect2i>()

        withOutputTextureOverride(framebuffer.colorTextureView, framebuffer.depthTextureView) {
            val matrices = Pools.MatStack.borrow()
            entities.forEachIndexed { index, (type, entity) ->
                val x = (index % itemsPerDimension) * ENTITY_TILE_SIZE
                val y = (index / itemsPerDimension) * ENTITY_TILE_SIZE
                val rect = Rect2i(x, y, ENTITY_TILE_SIZE, ENTITY_TILE_SIZE)
                entityMap[type] = rect

                runCatching { renderEntity(entity, matrices, x, y) }
                    .onFailure {
                        failedRects += rect
                        logger.warn(
                            "Unable to render entity preview for ${BuiltInRegistries.ENTITY_TYPE.getKey(type)}",
                            it,
                        )
                    }
            }
            Pools.MatStack.recycle(matrices)
        }

        RenderSystem.restoreProjectionMatrix()

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

    private fun renderEntity(entity: LivingEntity, matrices: com.mojang.blaze3d.vertex.PoseStack, x: Int, y: Int) {
        entity.yRot = 25F
        entity.yHeadRot = 25F
        entity.yBodyRot = 25F

        val state = mc.entityRenderDispatcher.extractEntity(entity, 1F)
        state.nameTag = null
        state.shadowPieces.clear()
        state.outlineColor = 0
        val scale = ENTITY_TILE_SIZE * 0.72F / max(entity.bbHeight, entity.bbWidth * 1.5F)

        matrices.pushPose()
        try {
            matrices.translate(x + ENTITY_TILE_SIZE * 0.5F, y + ENTITY_TILE_SIZE * 0.88F, 0F)
            matrices.scale(scale, -scale, scale)

            RenderSystem.enableScissorForRenderTypeDraws(
                x,
                textureSize - y - ENTITY_TILE_SIZE,
                ENTITY_TILE_SIZE,
                ENTITY_TILE_SIZE,
            )
            val cameraState = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
            mc.entityRenderDispatcher.submit(state, cameraState, 0.0, 0.0, 0.0, matrices, submitNodeCollector)
            featureRenderDispatcher.renderAllFeatures(submitNodeCollector)
        } finally {
            RenderSystem.disableScissorForRenderTypeDraws()
            matrices.popPose()
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

        entity?.id = BuiltInRegistries.ENTITY_TYPE.getId(type) + 1
        return entity
    }

    private fun close() {
        projectionMatrixBuffer.close()
        framebuffer.destroyBuffers()
        submitNodeCollector.drainPhases(Consumers.nop())
        featureRenderDispatcher.close()
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
