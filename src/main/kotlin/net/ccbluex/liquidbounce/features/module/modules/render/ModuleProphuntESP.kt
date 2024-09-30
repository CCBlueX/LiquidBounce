package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.utils.interpolateHue
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.toBlockPos
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.*

object ModuleProphuntESP : Module("ProphuntESP", Category.RENDER,
    aliases = arrayOf("BlockUpdateDetector", "FallingBlockESP")) {

    private val modes = choices(
        "Mode", Glow, arrayOf(
            Box, Glow, Outline
        )
    )

    private val colorMode = choices<GenericColorMode<Any>>(
        "ColorMode",
        { it.choices[0] },
        {
            arrayOf(
                ExpirationColor,
                GenericStaticColorMode(it, Color4b(255, 179, 72, 150)),
                GenericRainbowColorMode(it)
            )
        }
    )

    private object ExpirationColor : GenericColorMode<Any>("Expiration") {
        private val freshColor by color("FreshColor", Color4b(50, 200, 50, 255))
        private val expireColor by color("ExpireColor", Color4b(200, 50, 50, 255))

        override fun getColor(param: Any): Color4b =
            if (param is TrackedBlock) {
                interpolateHue(freshColor, expireColor, param.expirationProgress())
            } else {
                freshColor
            }

        override val parent: ChoiceConfigurable<*>
            get() = modes
    }

    private val renderBlockUpdates by boolean("RenderBlockUpdates", true)
    private val renderFallingBlockEntity by boolean("RenderFallingBlockEntity", true)

    private data class TrackedBlock(val pos: BlockPos, val expirationTime: Long) : Comparable<TrackedBlock> {
        override fun compareTo(other: TrackedBlock) =
            expirationTime.compareTo(other.expirationTime)

        fun expirationProgress() = Math.clamp(
            1 - (expirationTime - (mc.world?.time ?: 0)).toFloat() / renderTicks,
            0f,
            1f
        )
    }

    private val trackedBlocks = PriorityQueue<TrackedBlock>()
    private val renderTicks by float("RenderTicks", 60f, 0f..600f)

    @Suppress("unused")
    private val gameHandler = repeatable {
        synchronized(trackedBlocks) {
            while (trackedBlocks.isNotEmpty() && trackedBlocks.peek().expirationTime <= world.time) {
                trackedBlocks.poll()
            }
        }

        waitTicks(1)
    }

    private object Box : Choice("Box") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            drawBoxMode(event.matrixStack, this.outline, false)

            renderEnvironmentForWorld(event.matrixStack) {
                drawEntities(this, event.partialTicks, colorMode.activeChoice, true)
            }
        }
    }

    private fun drawBoxMode(matrixStack: MatrixStack, drawOutline: Boolean, fullAlpha: Boolean): Boolean {
        val colorMode = colorMode.activeChoice

        var dirty = false

        renderEnvironmentForWorld(matrixStack) {
            synchronized(trackedBlocks) {
                dirty = drawBlocks(this, trackedBlocks, colorMode, fullAlpha, drawOutline) || dirty
            }
        }

        return dirty
    }

    private fun WorldRenderEnvironment.drawEntities(
        env: WorldRenderEnvironment,
        partialTicks: Float,
        colorMode: GenericColorMode<Any>,
        drawOutline: Boolean
    ): Boolean {
        var dirty = false

        if (renderFallingBlockEntity) {
            BoxRenderer.drawWith(env) {
                mc.world?.entities?.filterIsInstance<FallingBlockEntity>()?.map {
                    val dimension = it.getDimensions(it.pose)
                    val width = dimension.width.toDouble() / 2.0
                    it to Box(-width, 0.0, -width, width, dimension.height.toDouble(), width)
                }?.forEach { (entity, box) ->
                    val pos = entity.interpolateCurrentPosition(partialTicks)
                    val color = colorMode.getColor(entity as Any) // which doesn't matter

                    val baseColor = color.alpha(50)
                    val outlineColor = color.alpha(100)

                    withPositionRelativeToCamera(pos) {
                        drawBox(
                            box,
                            baseColor,
                            outlineColor.takeIf { drawOutline }
                        )
                    }

                    dirty = true
                }
            }
        }

        return dirty
    }

    private fun WorldRenderEnvironment.drawBlocks(
        env: WorldRenderEnvironment,
        blocks: PriorityQueue<TrackedBlock>,
        colorMode: GenericColorMode<Any>,
        fullAlpha: Boolean,
        drawOutline: Boolean
    ): Boolean {
        var dirty = false

        BoxRenderer.drawWith(env) {

            if (renderBlockUpdates) {
                for (block in blocks) {
                    val pos = block.pos

                    val vec3d = Vec3d(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

                    val blockPos = vec3d.toBlockPos().toBlockPos()
                    val blockState = blockPos.getState() ?: continue

                    val outlineShape = blockState.getOutlineShape(world, blockPos)
                    val boundingBox = if (outlineShape.isEmpty) {
                        FULL_BOX
                    } else {
                        outlineShape.boundingBox
                    }

                    val color = colorMode.getColor(block)

                    if (fullAlpha) {
                        color.alpha(255)
                    }

                    withPositionRelativeToCamera(vec3d) {
                        drawBox(
                            boundingBox,
                            faceColor = color,
                            outlineColor = color.alpha(150).takeIf { drawOutline }
                        )
                    }

                    dirty = true
                }
            }
        }

        return dirty
    }

    private object Glow : Choice("Glow") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val renderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW) {
                return@handler
            }

            var dirty = drawBoxMode(event.matrixStack, drawOutline = false, fullAlpha = true)

            renderEnvironmentForWorld(event.matrixStack) {
                dirty = drawEntities(this, event.partialTicks, colorMode.activeChoice, true) || dirty
            }

            if (dirty) {
                event.markDirty()
            }
        }
    }

    private object Outline : Choice("Outline") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val renderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE) {
                return@handler
            }

            var dirty = drawBoxMode(event.matrixStack, drawOutline = false, fullAlpha = true)

            renderEnvironmentForWorld(event.matrixStack) {
                dirty = drawEntities(this, event.partialTicks, colorMode.activeChoice, true) || dirty
            }

            if (dirty) {
                event.markDirty()
            }
        }
    }

    @Suppress("unused")
    private val networkHandler = handler<PacketEvent> { event ->
        if (event.packet is BlockUpdateS2CPacket) {
            synchronized(trackedBlocks) {
                trackedBlocks.offer(TrackedBlock(event.packet.pos, world.time + renderTicks.toLong()))
            }
        }
    }
}
