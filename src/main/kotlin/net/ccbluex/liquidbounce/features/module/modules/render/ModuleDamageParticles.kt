package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.random.Random

object ModuleDamageParticles : Module("DamageParticles", Category.RENDER) {

    private val duration by int("Duration", 120, 20..360, "ticks")
    private val scale by float("Scale", 2F, 0.25F..4F)
    private val maximumDistance by float("MaximumDistance", 32F, 1F..256F)

    private val particles = hashSetOf<Particle>()
    private val hpMap = hashMapOf<LivingEntity, Float>()

    private val fontRenderer
        get() = Fonts.DEFAULT_FONT.get()

    override fun disable() {
        hpMap.clear()
    }

    val repeatable = repeatable {
        world.entities.filterIsInstance<LivingEntity>()
            .filter { player.distanceTo(it) <= maximumDistance }
            .forEach {
                if (!hpMap.containsKey(it)) {
                    hpMap[it] = it.health
                    return@forEach
                }

                val delta = it.health - hpMap[it]!!
                if (delta.absoluteValue < 1e-5) return@forEach

                particles.add(
                    Particle(
                        String.format("%.1f", delta.absoluteValue),
                        if (delta < 0) Color4b.RED else Color4b.GREEN,
                        it,
                        Vec3d(
                            Random.nextDouble(-0.3, 0.3) + it.velocity.x,
                            it.height * 0.6,
                            Random.nextDouble(-0.3, 0.3) + it.velocity.z
                        )
                    )
                )

                hpMap[it] = it.health
            }
    }

    val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            val renderer = Renderer()

            try {
                particles.removeIf { p ->
                    renderer.draw(p, this)
                    p.pos += Vec3d(0.0, cos(0.5 * PI * p.ticks / duration) * 0.5 * PI / duration, 0.0)
                    p.ticks++ > duration
                }
            } finally {
                renderer.commit(this)
            }
        }
    }

    private class Renderer {
        private val fontBuffers = FontRendererBuffers()

        fun draw(
            p: Particle,
            env: RenderEnvironment
        ) {
            val c = Fonts.DEFAULT_FONT_SIZE.toFloat()

            val scale = 1.0F / (c * 0.15F) * scale

            val pos = WorldToScreen.calculateScreenPos(p.pos + p.entity.pos) ?: return

            env.matrixStack.push()
            env.matrixStack.translate(pos.x, pos.y, pos.z)
            env.matrixStack.scale(scale, scale, 1.0F)

            val x =
                fontRenderer.draw(
                    p.text,
                    0.0F,
                    0.0F,
                    p.color,
                    shadow = true,
                    z = 0.001F,
                )

            env.matrixStack.translate(-x * 0.5F, -fontRenderer.height * 0.5F, 0.00F)
            fontRenderer.commit(env, fontBuffers)
            env.matrixStack.pop()
        }

        fun commit(env: RenderEnvironment) {
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
            GL11.glEnable(GL11.GL_DEPTH_TEST)

            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO
            )

            env.withColor(Color4b.WHITE) {
                fontBuffers.draw(ModuleNametags.fontRenderer)
            }
        }
    }

    private data class Particle(
        val text: String,
        val color: Color4b,
        val entity: LivingEntity,
        var pos: Vec3d,
        var ticks: Int = 0
    )

}
