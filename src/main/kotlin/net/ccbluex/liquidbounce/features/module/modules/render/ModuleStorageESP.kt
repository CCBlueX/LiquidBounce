@file:Suppress("LoopWithTooManyJumpStatements")
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.ModuleChestStealer
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.*
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.AbstractDonkeyEntity
import net.minecraft.entity.vehicle.ChestBoatEntity
import net.minecraft.entity.vehicle.HopperMinecartEntity
import net.minecraft.entity.vehicle.StorageMinecartEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.awt.Color

/**
 * StorageESP module
 *
 * Allows you to see chests, dispensers, etc. through walls.
 */

object ModuleStorageESP : ClientModule("StorageESP", Category.RENDER, aliases = arrayOf("ChestESP")) {

    private val modes = choices("Mode", TagMode, arrayOf(BoxMode, Glow, TagMode))
    private val tracers by multiEnumChoice<ChestType>("Tracers")

    private val chestColor by color("Chest", Color4b.WHITE)
    private val enderChestColor by color("EnderChest", Color4b(Color.MAGENTA))
    private val furnaceColor by color("Furnace", Color4b(79, 79, 79))
    private val dispenserColor by color("Dispenser", Color4b(Color.LIGHT_GRAY))
    private val hopperColor by color("Hopper", Color4b(Color.GRAY))
    private val shulkerColor by color("ShulkerBox", Color4b(Color(0x6e, 0x4d, 0x6e).brighter()))
    private val potColor by color("Pot", Color4b(209, 134, 0))

    private val requiresChestStealer by boolean("RequiresChestStealer", false)
    private val show by multiEnumChoice("Show", Show.CHEST)
    override fun onEnabled() {
        ChunkScanner.subscribe(StorageScanner)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(StorageScanner)
    }

    private object TagMode : Choice("Tag") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val backgroundColor by color("BackgroundColor", Color4b(Int.MIN_VALUE, hasAlpha = true))
        private val scale by float("Scale", 0.5F, 0.25F..1F)

        @Suppress("unused")
        val renderHandler = handler<OverlayRenderEvent> { event ->
            renderEnvironmentForGUI {
                for ((pos, type) in StorageScanner.iterate()) {
                    if (type.showChoice !in show || type.color.a <= 0 || !type.shouldRender(pos)) continue
                    val state = pos.getState() ?: continue
                    if (state.isAir) continue

                    val worldPos = pos.toVec3d().add(0.5, 0.5, 0.5)
                    val screenPos = WorldToScreen.calculateScreenPos(worldPos) ?: continue

                    val stack = when (type) {
                        ChestType.CHEST -> ItemStack(Items.CHEST)
                        ChestType.ENDER_CHEST -> ItemStack(Items.ENDER_CHEST)
                        ChestType.FURNACE -> ItemStack(Items.FURNACE)
                        ChestType.DISPENSER -> ItemStack(Items.DISPENSER)
                        ChestType.HOPPER -> ItemStack(Items.HOPPER)
                        ChestType.SHULKER_BOX -> ItemStack(Items.SHULKER_BOX)
                        ChestType.POT -> ItemStack(Items.DECORATED_POT)
                    }

                    event.context.drawItemTags(
                        stacks = listOf(stack),
                        centerPos = screenPos,
                        backgroundColor = backgroundColor.toARGB(),
                        scale = scale,
                        rowLength = 1
                    )
                }

                for (entity in world.entities) {
                    val type = entity.categorize() ?: continue
                    if (type.showChoice !in show || type.color.a <= 0) continue

                    val pos = entity.interpolateCurrentPosition(event.tickDelta)
                    val screenPos = WorldToScreen.calculateScreenPos(pos) ?: continue

                    val stack = when (type) {
                        ChestType.CHEST -> ItemStack(Items.CHEST)
                        ChestType.HOPPER -> ItemStack(Items.HOPPER)
                        else -> continue
                    }

                    event.context.drawItemTags(
                        stacks = listOf(stack),
                        centerPos = screenPos,
                        backgroundColor = backgroundColor.toARGB(),
                        scale = scale,
                        rowLength = 1
                    )
                }
            }
        }
    }

    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)
        private val fadeDistance by float("FadeDistance", 64f, 16f..256f, "blocks")
        private val minAlpha by int("MinAlpha", 40, 0..255)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val queuedBoxes = collectBoxesToDraw(event)

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    for ((pos, box, color) in queuedBoxes) {
                        val playerPos = player.interpolateCurrentPosition(event.partialTicks)
                        val dist = playerPos.distanceTo(pos)
                        val alphaFactor =
                            if (dist >= fadeDistance) {
                                1f
                            } else {
                                minAlpha / 255f + (1f - minAlpha / 255f) * (dist / fadeDistance)
                            }
                        val baseAlpha = (50 * alphaFactor.toFloat()).toInt().coerceIn(0, 255)
                        val outlineAlpha = (100 * alphaFactor.toFloat()).toInt().coerceIn(0, 255)

                        val baseColor = color.with(a = baseAlpha)
                        val outlineColor = color.with(a = outlineAlpha)

                        withPositionRelativeToCamera(pos) {
                            drawBox(box, baseColor, outlineColor.takeIf { outline })
                        }
                    }
                }
            }
        }

        @JvmRecord
        private data class BoxRecord(val pos: Vec3d, val box: Box, val color: Color4b)


        private fun collectBoxesToDraw(event: WorldRenderEvent): List<BoxRecord> {
            val queuedBoxes = mutableListOf<BoxRecord>()

            for ((pos, type) in StorageScanner.iterate()) {
                if (type.showChoice !in show) continue
                val color = type.color
                if (color.a <= 0 || !type.shouldRender(pos)) continue

                val state = pos.getState() ?: continue
                if (state.isAir) continue

                val outlineShape = state.getOutlineShape(world, pos)
                val boundingBox = if (outlineShape.isEmpty) FULL_BOX else outlineShape.boundingBox

                queuedBoxes.add(BoxRecord(pos.toVec3d(), boundingBox, color))
            }

            for (entity in world.entities) {
                val type = entity.categorize() ?: continue
                if (type.showChoice !in show) continue

                val pos = entity.interpolateCurrentPosition(event.partialTicks)
                val dimensions = entity.getDimensions(entity.pose)
                val d = dimensions.width.toDouble() / 2.0
                val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

                queuedBoxes.add(BoxRecord(pos, box, type.color))
            }

            return queuedBoxes
        }
    }

    object Glow : Choice("Glow") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        val glowRenderHandler = handler<DrawOutlinesEvent> { event ->
            if (event.type != DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW
                || StorageScanner.isEmpty()) {
                return@handler
            }

            renderEnvironmentForWorld(event.matrixStack) {
                BoxRenderer.drawWith(this) {
                    for ((pos, type) in StorageScanner.iterate()) {
                        val state = pos.getState() ?: continue

                        // non-model blocks are already processed by WorldRenderer where we injected code which renders
                        // their outline
                        if (state.renderType != BlockRenderType.MODEL) {
                            continue
                        }

                        if (state.isAir) {
                            continue
                        }

                        val outlineShape = state.getOutlineShape(world, pos)

                        val boundingBox = if (outlineShape.isEmpty) {
                            FULL_BOX
                        } else {
                            outlineShape.boundingBox
                        }

                        withPosition(relativeToCamera(Vec3d.of(pos))) {
                            drawBox(boundingBox, type.color)
                        }

                        event.markDirty()
                    }
                }
            }
        }
    }
    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (StorageScanner.isEmpty() || tracers.isEmpty()) {
            return@handler
        }

        renderEnvironmentForWorld(event.matrixStack) {
            val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            longLines {
                for ((blockPos, type) in StorageScanner.iterate()) {
                    if (type !in tracers) continue
                    val pos = relativeToCamera(blockPos.toCenterPos()).toVec3()

                    withColor(type.color) {
                        drawLines(eyeVector, pos, pos)
                    }
                }
            }
        }
    }
    @JvmStatic
    fun Entity.categorize(): ChestType? {
        return when (this) {
            // This includes any storage type minecart entity including ChestMinecartEntity
            is HopperMinecartEntity -> ChestType.HOPPER
            is StorageMinecartEntity -> ChestType.CHEST
            is ChestBoatEntity -> ChestType.CHEST
            is AbstractDonkeyEntity -> ChestType.CHEST.takeIf { hasChest() }
            else -> null
        }
    }

    @JvmStatic
    fun BlockEntity.categorize(): ChestType? {
        return when (this) {
            is ChestBlockEntity -> ChestType.CHEST
            is EnderChestBlockEntity -> ChestType.ENDER_CHEST
            is AbstractFurnaceBlockEntity -> ChestType.FURNACE
            is DispenserBlockEntity -> ChestType.DISPENSER
            is HopperBlockEntity -> ChestType.HOPPER
            is ShulkerBoxBlockEntity -> ChestType.SHULKER_BOX
            is BarrelBlockEntity -> ChestType.CHEST
            is DecoratedPotBlockEntity -> ChestType.POT
            else -> null
        }
    }

    enum class Show(override val choiceName: String) : NamedChoice {
        CHEST("Chest"),
        ENDER_CHEST("EnderChest"),
        FURNACE("Furnace"),
        DISPENSER("Dispenser"),
        HOPPER("Hopper"),
        SHULKER_BOX("ShulkerBox"),
        POT("Pot"),
    }

    enum class ChestType(val showChoice: Show) {
        CHEST(Show.CHEST) {
            override val color get() = chestColor
            override fun shouldRender(pos: BlockPos) = pos !in FeatureChestAura.interactedBlocksSet
        },
        ENDER_CHEST(Show.ENDER_CHEST) {
            override val color get() = enderChestColor
            override fun shouldRender(pos: BlockPos) = pos !in FeatureChestAura.interactedBlocksSet
        },
        FURNACE(Show.FURNACE) {
            override val color get() = furnaceColor
        },
        DISPENSER(Show.DISPENSER) {
            override val color get() = dispenserColor
        },
        HOPPER(Show.HOPPER) {
            override val color get() = hopperColor
        },
        SHULKER_BOX(Show.SHULKER_BOX) {
            override val color get() = shulkerColor
            override fun shouldRender(pos: BlockPos) = pos !in FeatureChestAura.interactedBlocksSet
        },
        POT(Show.POT) {
            override val color get() = potColor
        };
        abstract val color: Color4b
        open fun shouldRender(pos: BlockPos): Boolean = true
    }

    private object StorageScanner : AbstractBlockLocationTracker.State2BlockPos<ChestType>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): ChestType? {
            val chunk = mc.world?.getChunk(pos) ?: return null
            return chunk.getBlockEntity(pos)?.categorize()
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
