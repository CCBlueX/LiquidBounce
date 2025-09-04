package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

object ModuleProjectileESP : ClientModule("ProjectileESP", Category.RENDER, aliases = arrayOf("PearlESP")) {
    private val scale by float("Scale", 0.5F, 0.25F..1F)

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
