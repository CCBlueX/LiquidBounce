/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer.Companion.getColorIndex
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.hitBox
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.renderer.GlStateManager.enableTexture2D
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@ModuleInfo(name = "ESP", description = "Allows you to see targets through walls.", category = ModuleCategory.RENDER)
class ESP : Module() {
    @JvmField
    val modeValue = ListValue(
        "Mode",
        arrayOf("Box", "OtherBox", "WireFrame", "2D", "Real2D", "Outline", "Glow"),
        "Box"
    )

    @JvmField
    val outlineWidth = object : FloatValue("Outline-Width", 3f, 0.5f, 5f) {
        override fun isSupported() = modeValue.get() == "Outline"
    }

    @JvmField
    val wireframeWidth = object : FloatValue("WireFrame-Width", 2f, 0.5f, 5f) {
        override fun isSupported() = modeValue.get() == "WireFrame"
    }

    private val glowRenderScale = object : FloatValue("Glow-Renderscale", 1f, 0.1f, 2f) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowRadius = object : IntegerValue("Glow-Radius", 4, 1, 5) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowFade = object : IntegerValue("Glow-Fade", 10, 0, 30) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowTargetAlpha = object : FloatValue("Glow-Target-Alpha", 0f, 0f, 1f) {
        override fun isSupported() = modeValue.get() == "Glow"
    }

    private val colorRainbow = BoolValue("Rainbow", false)
    private val colorRedValue = object : IntegerValue("R", 255, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorGreenValue = object : IntegerValue("G", 255, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorBlueValue = object : IntegerValue("B", 255, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }

    private val colorTeam = BoolValue("Team", false)
    private val botValue = BoolValue("Bots", true)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val mode = modeValue.get()
        val mvMatrix = WorldToScreen.getMatrix(GL_MODELVIEW_MATRIX)
        val projectionMatrix = WorldToScreen.getMatrix(GL_PROJECTION_MATRIX)
        val real2d = mode == "Real2D"

        if (real2d) {
            glPushAttrib(GL_ENABLE_BIT)
            glEnable(GL_BLEND)
            glDisable(GL_TEXTURE_2D)
            glDisable(GL_DEPTH_TEST)
            glMatrixMode(GL_PROJECTION)
            glPushMatrix()
            glLoadIdentity()
            glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), 0.0, -1.0, 1.0)
            glMatrixMode(GL_MODELVIEW)
            glPushMatrix()
            glLoadIdentity()
            glDisable(GL_DEPTH_TEST)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            enableTexture2D()
            glDepthMask(true)
            glLineWidth(1f)
        }

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !botValue.get() && isBot(entity)) continue
            if (entity != mc.thePlayer && isSelected(entity, false)) {
                val color = getColor(entity)

                when (mode.lowercase()) {
                    "box", "otherbox" -> drawEntityBox(entity, color, mode != "OtherBox")
                    "2d" -> {
                        val renderManager = mc.renderManager
                        val timer = mc.timer
                        val posX: Double =
                            entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX
                        val posY: Double =
                            entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY
                        val posZ: Double =
                            entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
                        draw2D(entity, posX, posY, posZ, color.rgb, Color.BLACK.rgb)
                    }
                    "real2d" -> {
                        val renderManager = mc.renderManager
                        val timer = mc.timer
                        val bb = entity.hitBox
                            .offset(-entity.posX, -entity.posY, -entity.posZ)
                            .offset(
                                entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * timer.renderPartialTicks,
                                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * timer.renderPartialTicks,
                                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * timer.renderPartialTicks
                            )
                            .offset(-renderManager.renderPosX, -renderManager.renderPosY, -renderManager.renderPosZ)
                        val boxVertices = arrayOf(
                            doubleArrayOf(bb.minX, bb.minY, bb.minZ),
                            doubleArrayOf(bb.minX, bb.maxY, bb.minZ),
                            doubleArrayOf(bb.maxX, bb.maxY, bb.minZ),
                            doubleArrayOf(bb.maxX, bb.minY, bb.minZ),
                            doubleArrayOf(bb.minX, bb.minY, bb.maxZ),
                            doubleArrayOf(bb.minX, bb.maxY, bb.maxZ),
                            doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ),
                            doubleArrayOf(bb.maxX, bb.minY, bb.maxZ)
                        )
                        var minX = Float.MAX_VALUE
                        var minY = Float.MAX_VALUE
                        var maxX = -1f
                        var maxY = -1f
                        for (boxVertex in boxVertices) {
                            val screenPos = WorldToScreen.worldToScreen(
                                Vector3f(
                                    boxVertex[0].toFloat(),
                                    boxVertex[1].toFloat(),
                                    boxVertex[2].toFloat()
                                ), mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight
                            )
                                ?: continue
                            minX = min(screenPos.x, minX)
                            minY = min(screenPos.y, minY)
                            maxX = max(screenPos.x, maxX)
                            maxY = max(screenPos.y, maxY)
                        }
                        if (minX > 0 || minY > 0 || maxX <= mc.displayWidth || maxY <= mc.displayWidth) {
                            glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
                            glBegin(GL_LINE_LOOP)
                            glVertex2f(minX, minY)
                            glVertex2f(minX, maxY)
                            glVertex2f(maxX, maxY)
                            glVertex2f(maxX, minY)
                            glEnd()
                        }
                    }
                }
            }
        }

        if (real2d) {
            glEnable(GL_DEPTH_TEST)
            glMatrixMode(GL_PROJECTION)
            glPopMatrix()
            glMatrixMode(GL_MODELVIEW)
            glPopMatrix()
            glPopAttrib()
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = modeValue.get().lowercase()
        val partialTicks = event.partialTicks
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return
        shader.startDraw(event.partialTicks, glowRenderScale.get())
        renderNameTags = false

        if(mc.theWorld == null) return

        try {
            val entityMap = mutableMapOf<Color, ArrayList<Entity>>()
            mc.theWorld.loadedEntityList
                .filter { isSelected(it, false) }
                .filterIsInstance<EntityLivingBase>()
                .filterNot { isBot(it) && botValue.get() }.forEach { entity ->
                val color = getColor(entity)
                if (color !in entityMap) {
                    entityMap[color] = ArrayList()
                }
                entityMap[color]!!.add(entity)
            }

            entityMap.forEach { (color, entities) ->
                shader.startDraw(partialTicks, glowRenderScale.get())
                for (entity in entities) {
                    mc.renderManager.renderEntitySimple(entity, partialTicks)
                }
                shader.stopDraw(color, glowRadius.get(), glowFade.get(), glowTargetAlpha.get())
            }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all entities for shader esp", ex)
        }
        renderNameTags = true
        shader.stopDraw(getColor(null), glowRadius.get(), glowFade.get(), glowTargetAlpha.get())
    }

    override val tag
        get() = modeValue.get()

    fun getColor(entity: Entity?): Color {
        run {
            if (entity != null && entity is EntityLivingBase) {
                if (entity.hurtTime > 0)
                    return Color.RED
                if (entity is EntityPlayer && entity.isClientFriend())
                    return Color.BLUE

                if (colorTeam.get()) {
                    val chars: CharArray = (entity.displayName ?: return@run).formattedText.toCharArray()
                    var color = Int.MAX_VALUE
                    for (i in chars.indices) {
                        if (chars[i] != '§' || i + 1 >= chars.size) continue
                        val index = getColorIndex(chars[i + 1])
                        if (index < 0 || index > 15) continue
                        color = ColorUtils.hexColors[index]
                        break
                    }

                    return Color(color)
                }
            }
        }

        return if (colorRainbow.get()) rainbow() else Color(
            colorRedValue.get(),
            colorGreenValue.get(),
            colorBlueValue.get()
        )
    }

    companion object {
        @JvmField
        var renderNameTags = true
    }
}