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
package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.atomicfu.atomic
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.RenderPassRenderState
import net.ccbluex.liquidbounce.render.addBoxFaces
import net.ccbluex.liquidbounce.render.addBoxOutlines
import net.ccbluex.liquidbounce.render.buildMesh
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawGenericBlockESP
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.getDynamicTransformsUniform
import net.ccbluex.liquidbounce.render.longLines
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.translate
import net.ccbluex.liquidbounce.render.utils.DistanceFadeUniformValueGroup
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse
import net.minecraft.world.entity.vehicle.boat.ChestBoat
import net.minecraft.world.entity.vehicle.boat.ChestRaft
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.CrafterBlockEntity
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity
import net.minecraft.world.level.block.entity.DispenserBlockEntity
import net.minecraft.world.level.block.entity.EnderChestBlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.entity.ShelfBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import java.awt.Color

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : ClientModule("StorageESP", ModuleCategories.RENDER, aliases = listOf("ChestESP")) {

    private val modes = choices("Mode", GlowMode, arrayOf(BoxMode, GlowMode))

    sealed class ChestType(name: String, defaultColor: Color4b) : ToggleableValueGroup(this, name, enabled = true) {
        val color by color("Color", defaultColor)
        val tracers by boolean("Tracers", false)

        @JvmOverloads
        fun shouldRender(pos: BlockPos, ignoreDistance: Boolean = false): Boolean =
            this.running
                && pos !in FeatureChestAura.interactedBlocksSet
                && (ignoreDistance || pos.cameraDistanceSq() < distanceFade.farEnd.sq())

        @JvmOverloads
        fun shouldRender(entity: Entity, ignoreDistance: Boolean = false): Boolean =
            this.running
                && (ignoreDistance || entity.position().cameraDistanceSq() < distanceFade.farEnd.sq())

        object Chest : ChestType("Chest", Color4b(0, 100, 255))
        object EnderChest : ChestType("EnderChest", Color4b(Color.MAGENTA))
        object Furnace : ChestType("Furnace", Color4b(79, 79, 79))
        object BrewingStand : ChestType("BrewingStand", Color4b(139, 69, 19))
        object Dispenser : ChestType("Dispenser", Color4b(Color.LIGHT_GRAY))
        object Hopper : ChestType("Hopper", Color4b(Color.GRAY))
        object ShulkerBox : ChestType("ShulkerBox", Color4b(Color(0x6e, 0x4d, 0x6e).brighter()))
        object Pot : ChestType("Pot", Color4b(209, 134, 0))
        object Shelf : ChestType("Shelf", Color4b(160, 82, 45))
    }

    private val allTypes = arrayOf(
        ChestType.Chest,
        ChestType.EnderChest,
        ChestType.Furnace,
        ChestType.BrewingStand,
        ChestType.Dispenser,
        ChestType.Hopper,
        ChestType.ShulkerBox,
        ChestType.Pot,
        ChestType.Shelf,
    )

    init {
        allTypes.forEach { tree(it) }
    }

    private val requiresChestStealer by boolean("RequiresChestStealer", false)

    private val distanceFade = tree(DistanceFadeUniformValueGroup())

    override fun onEnabled() {
        ChunkScanner.subscribe(StorageScanner)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(StorageScanner)
    }

    private object BoxMode : Mode("Box") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        val dirtyFlag = atomic(true)

        private val blockFacesRenderState = RenderPassRenderState("${ModuleStorageESP.name} $name BlockFaces")
        private val blockOutlinesRenderState = RenderPassRenderState("${ModuleStorageESP.name} $name BlockOutlines")

        override fun enable() {
            dirtyFlag.value = true
            super.enable()
        }

        private val outline by boolean("Outline", true)

        private val entityBoxes = mutableListOf<EntityBox>()

        override fun disable() {
            blockFacesRenderState.clearStates()
            blockFacesRenderState.clearBuffers()
            blockOutlinesRenderState.clearStates()
            blockOutlinesRenderState.clearBuffers()
            entityBoxes.clear()
            super.disable()
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (outline) {
                mc.mainRenderTarget.drawGenericBlockESP(
                    renderState = blockOutlinesRenderState,
                    pipeline = ClientRenderPipelines.relativeLines(useColor = true),
                    distanceFade = distanceFade,
                ) {
                    getDynamicTransformsUniform(modelView = event.matrixStack.last().pose())
                }
            }

            mc.mainRenderTarget.drawGenericBlockESP(
                renderState = blockFacesRenderState,
                pipeline = ClientRenderPipelines.relativeQuads(useColor = true),
                distanceFade = distanceFade,
            ) {
                getDynamicTransformsUniform(modelView = event.matrixStack.last().pose())
            }

            if (entityBoxes.isEmpty()) return@handler

            val matrixStack = event.matrixStack

            renderEnvironmentForWorld(matrixStack) {
                startBatch()

                for ((entity, box, color) in entityBoxes) {
                    val baseColor = color.with(a = 50)
                    val outlineColor = if (outline) color.with(a = 100) else null

                    val pos = entity.interpolateCurrentPosition(event.partialTicks)
                    withPositionRelativeToCamera(pos) {
                        drawBox(box, baseColor, outlineColor)
                    }
                }

                commitBatch()
            }
        }

        @JvmRecord
        private data class EntityBox(val entity: Entity, val box: AABB, val color: Color4b)

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            val level = mc.level ?: return@handler

            entityBoxes.clear()

            for (entity in level.entitiesForRendering()) {
                val type = entity.categorize()?.takeIf {
                    !it.color.isTransparent && it.shouldRender(entity)
                } ?: continue

                val dimensions = entity.getDimensions(entity.pose)
                val d = dimensions.width.toDouble() / 2.0
                val box = AABB(-d, 0.0, -d, d, dimensions.height.toDouble(), d).inflate(0.05)

                entityBoxes.add(EntityBox(entity, box, type.color))
            }


            if (StorageScanner.isEmpty()) {
                blockFacesRenderState.clearStates()
                blockOutlinesRenderState.clearStates()
                return@handler
            }

            if (!dirtyFlag.compareAndSet(expect = true, update = false)) {
                return@handler
            }

            blockFacesRenderState.buildMesh(
                pipeline = ClientRenderPipelines.relativeQuads(useColor = true),
            ) { pose ->
                forEachTrackedBlockBoxes { blockPos, type, outlineBox ->
                    pose.withPush {
                        translate(blockPos)
                        addBoxFaces(last().pose(), outlineBox, type.color.alpha(50))
                    }
                }
            }

            if (outline) {
                blockOutlinesRenderState.buildMesh(
                    pipeline = ClientRenderPipelines.relativeLines(useColor = true),
                ) { pose ->
                    forEachTrackedBlockBoxes { blockPos, type, outlineBox ->
                        pose.withPush {
                            translate(blockPos)
                            addBoxOutlines(last().pose(), outlineBox, type.color.alpha(100))
                        }
                    }
                }
            }
        }

    }

    object GlowMode : Mode("Glow") {
        internal val dirtyFlag = atomic(true)

        private val renderState = RenderPassRenderState("${ModuleStorageESP.name} $name")

        override fun enable() {
            dirtyFlag.value = true
            super.enable()
        }

        override fun disable() {
            renderState.clearStates()
            renderState.clearBuffers()
            super.disable()
        }

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val glowRenderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW) {
                return@handler
            }

            val dirty = event.renderTarget.drawGenericBlockESP(
                renderState = renderState,
                pipeline = ClientRenderPipelines.outlineQuads(useColor = true),
                distanceFade = distanceFade,
            )

            if (dirty) {
                event.markDirty()
            }
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            if (StorageScanner.isEmpty()) {
                renderState.clearStates()
                return@handler
            }

            if (!dirtyFlag.compareAndSet(expect = true, update = false)) {
                return@handler
            }

            renderState.buildMesh(
                pipeline = ClientRenderPipelines.outlineQuads(useColor = true),
            ) { pose ->
                // non-model blocks are already processed by WorldRenderer where we injected code which renders
                // their outline
                forEachTrackedBlockBoxes({ it.renderShape != RenderShape.MODEL }) { blockPos, type, outlineBox ->
                    pose.withPush {
                        translate(blockPos)
                        addBoxFaces(last().pose(), outlineBox, type.color)
                    }
                }
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (StorageScanner.isEmpty()) return@handler

        val types = allTypes.filter { it.tracers && !it.color.isTransparent }
        if (types.isEmpty()) return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            val eyeVector = Vec3f(0.0, 0.0, 1.0)
                .rotateX(-camera.xRot().toRadians())
                .rotateY(-camera.yRot().toRadians())

            startBatch()
            longLines {
                for (type in types) {
                    for (blockPos in StorageScanner.iterate(type)) {
                        if (!type.shouldRender(blockPos)) continue
                        val pos = relativeToCamera(blockPos.center).toVec3f()

                        drawLine(eyeVector, pos, type.color.argb)
                    }
                }
            }
            commitBatch()
        }
    }

    @JvmStatic
    fun Entity?.categorize(): ChestType? {
        return when (this) {
            // This includes any storage type minecart entity including ChestMinecartEntity
            is MinecartHopper -> ChestType.Hopper
            is AbstractMinecartContainer -> ChestType.Chest
            is ChestBoat -> ChestType.Chest
            is ChestRaft -> ChestType.Chest
            is AbstractChestedHorse -> ChestType.Chest.takeIf { hasChest() }
            else -> null
        }
    }

    @JvmStatic
    fun BlockEntity?.categorize(): ChestType? {
        return when (this) {
            is ChestBlockEntity, is BarrelBlockEntity -> ChestType.Chest
            is EnderChestBlockEntity -> ChestType.EnderChest
            is AbstractFurnaceBlockEntity -> ChestType.Furnace
            is BrewingStandBlockEntity -> ChestType.BrewingStand
            is DispenserBlockEntity -> ChestType.Dispenser
            is CrafterBlockEntity -> ChestType.Dispenser
            is HopperBlockEntity -> ChestType.Hopper
            is ShulkerBoxBlockEntity -> ChestType.ShulkerBox
            is DecoratedPotBlockEntity -> ChestType.Pot
            is ShelfBlockEntity -> ChestType.Shelf
            else -> null
        }
    }

    private inline fun forEachTrackedBlockBoxes(
        skipWhen: (BlockState) -> Boolean = { false },
        block: (blockPos: BlockPos, type: ChestType, outlineBox: AABB) -> Unit,
    ) {
        for ((blockPos, type) in StorageScanner.iterate()) {
            if (type.color.isTransparent || !type.shouldRender(blockPos, ignoreDistance = true)) continue

            val blockState = world.getBlockState(blockPos)

            if (blockState.isAir || skipWhen(blockState)) continue

            val boundingBox = blockState.outlineBox(blockPos)

            block(blockPos, type, boundingBox)
        }
    }

    private object StorageScanner : AbstractBlockLocationTracker.State2BlockPos<ChestType>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): ChestType? {
            val chunk = mc.level?.getChunk(pos) ?: return null
            return chunk.getBlockEntity(pos)?.categorize()
        }

        override fun onUpdated() {
            GlowMode.dirtyFlag.value = true
            BoxMode.dirtyFlag.value = true
        }
    }

    override val running: Boolean
        get() {
            if (requiresChestStealer && !ModuleChestStealer.running) {
                return false
            }

            return super.running
        }

}
