package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

object ModuleProjectileESP : ClientModule("ProjectileESP", Category.RENDER, aliases = arrayOf("PearlESP")) {
    private val scale by float("Scale", 0.5F, 0.25F..1F)

    private object ShowLandingMarker : ToggleableConfigurable(this,"ShowLandingMarker",true){
        private val opacity by float("Opacity", 1.0F, 0.25F..1F)
        private val pointerTexture: Identifier = "image/waypoint.png".registerAsDynamicImageFromClientResources()

        @Suppress("unused")
        private val worldRenderHandler = handler<WorldRenderEvent> { event ->
            val pearls = world.entities.filterIsInstance<EnderPearlEntity>()
            if (pearls.isEmpty()){
                return@handler
            }
            if (!enabled) {
                return@handler
            }

            renderEnvironmentForWorld(event.matrixStack) {
                pearls.forEach { pearl ->

                    val renderer = TrajectoryInfoRenderer(
                        pearl,
                        pearl.velocity,
                        pearl.pos,
                        TrajectoryInfo.GENERIC,
                        TrajectoryInfoRenderer.Type.REAL,
                        Vec3d.ZERO
                    )
                    val landingPos = renderer.runSimulation(300).hitResult?.pos ?: return@forEach

                    val renderPos = landingPos.add(0.0, 0.5, 0.0)
                    RenderSystem.setShaderTexture(0, pointerTexture)

                    withPositionRelativeToCamera(renderPos) {
                        matrixStack.apply {
                            multiply(mc.gameRenderer.camera.rotation)
                            scale(1f, 1f, 1f)
                            translate(-0.5f, -0.5f, 0f)
                        }

                        val alpha = MathHelper.clamp((255 * opacity).toInt(), 0, 255)
                        val color = Color4b.WHITE.withAlpha(alpha)

                        drawCustomMesh(
                            VertexFormat.DrawMode.QUADS,
                            VertexFormats.POSITION_TEXTURE_COLOR,
                            ShaderProgramKeys.POSITION_TEX_COLOR
                        ) { matrix ->
                            vertex(matrix, 0f, 0f, 0f).texture(0f, 1f).color(color.toARGB())
                            vertex(matrix, 1f, 0f, 0f).texture(1f, 1f).color(color.toARGB())
                            vertex(matrix, 1f, 1f, 0f).texture(1f, 0f).color(color.toARGB())
                            vertex(matrix, 0f, 1f, 0f).texture(0f, 0f).color(color.toARGB())
                        }
                    }
                }
            }
        }
    }
    init {
        tree(ShowLandingMarker)
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        renderEnvironmentForGUI {
            world.entities.forEach { entity ->
                if (entity !is EnderPearlEntity) return@forEach

                val pos = entity.interpolateCurrentPosition(event.tickDelta)
                val screenPos = WorldToScreen.calculateScreenPos(pos) ?: return@forEach

                event.context.drawItemStackList(listOf(ItemStack(Items.ENDER_PEARL)))
                    .centerX(screenPos.x)
                    .centerY(screenPos.y)
                    .centerZ(screenPos.z)
                    .scale(scale)
                    .rectBackground(0)
                    .rowLength(1)
                    .drawStackOverlay(false)
                    .draw()
            }
        }
    }

}
