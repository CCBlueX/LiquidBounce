package net.ccbluex.liquidbounce.features.module.modules.world.autoFarm

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

object AutoFarmVisualizer : ToggleableConfigurable(ModuleAutoFarm, "Visualize", true) {
    private object Path : ToggleableConfigurable(this.module, "Path", true) {
        val color by color("PathColor", Color4b(36, 237, 0, 255))

        val renderHandler = handler<WorldRenderEvent> { event ->
            renderEnvironmentForWorld(event.matrixStack){
                withColor(color){
                    ModuleAutoFarm.walkTarget?.let { target ->
                        drawLines(player.interpolateCurrentPosition(event.partialTicks).toVec3(), Vec3(target))
                    }
                }
            }

        }

    }

    private object Blocks : ToggleableConfigurable(this.module, "Blocks", true) {
        val outline by boolean("Outline", true)

        private val readyColor by color("ReadyColor", Color4b(36, 237, 0, 255))
        private val placeColor by color("PlaceColor", Color4b(191, 245, 66, 100))
        private val range by int("Range", 50, 10..128).listen {
            rangeSquared = it * it
            it
        }
        var rangeSquared: Int = range * range


        private val colorRainbow by boolean("Rainbow", false)

        // todo: use box of block, not hardcoded
        private val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        private object CurrentTarget : ToggleableConfigurable(this.module, "CurrentTarget", true) {
            private val color by color("Color", Color4b(66, 120, 245, 255))
            private val colorRainbow by boolean("Rainbow", false)

            fun render(renderEnvironment: RenderEnvironment) {
                if(!this.enabled) return
                val target = ModuleAutoFarm.currentTarget ?: return
                with(renderEnvironment){
                    withPosition(Vec3(target)){
                        withColor((if(colorRainbow) rainbow() else color).alpha(50)){
                            drawSolidBox(box)
                        }
                    }
                }
            }
        }


        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val baseColor = if (colorRainbow) rainbow() else readyColor
//                val baseForFarmBlocks = if (colorRainbow) rainbow() else farmBlockColor

            val fillColor = baseColor.alpha(50)
            val outlineColor = baseColor.alpha(100)


            val markedBlocks = AutoFarmBlockTracker.trackedBlockMap
//                val markedFarmBlocks = FarmBlockTracker.trackedBlockMap.keys
            renderEnvironmentForWorld(matrixStack) {
                CurrentTarget.render(this)
                for ((pos, type) in markedBlocks) {
                    val vec3 = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
                    val xdiff = pos.x - player.x
                    val zdiff = pos.z - player.z
                    if (xdiff * xdiff + zdiff * zdiff > rangeSquared) continue

                    withPosition(vec3) {
                        if(type == AutoFarmTrackedStates.Destroy){
                            withColor(fillColor) {
                                drawSolidBox(box)
                            }
                        } else {
                            withColor(placeColor) {
                                drawSideBox(box, Direction.UP)
                            }

                        }

                        if (outline && type == AutoFarmTrackedStates.Destroy) {
                            withColor(outlineColor) {
                                drawOutlinedBox(box)
                            }
                        }
                    }
                }
            }
        }
    }
    init {
        tree(Path)
        tree(Blocks)
    }
}
