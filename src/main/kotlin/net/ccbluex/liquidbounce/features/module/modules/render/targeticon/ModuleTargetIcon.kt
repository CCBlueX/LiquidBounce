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
package net.ccbluex.liquidbounce.features.module.modules.render.targeticon

import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.drawTexQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.textureSetup
import net.minecraft.world.entity.LivingEntity

object ModuleTargetIcon : ClientModule("TargetIcon", ModuleCategories.RENDER) {

    private val onlyKillAura by boolean("OnlyKillAura", false)
    private val icon by enumChoice("Icon", TargetIconRegistry.NONE)
    private val randomIcon by boolean("RandomIcon", false)
    private val iconSize by int("Size", 80, 16..256)
    private const val DISPLAY_DURATION = 1000L

    private val heightFraction by float("Height", 0.85f, 0f..1.5f)
    private val offsetX by float("OffsetX", 0f, -200f..200f)
    private val offsetY by float("OffsetY", -10f, -200f..200f)

    @Volatile private var trackedEntity: LivingEntity? = null
    @Volatile private var activeIcon: TargetIconRegistry = TargetIconRegistry.NONE
    @Volatile private var showUntil: Long = 0L
    @Volatile private var shownSince: Long = 0L

    override fun onDisabled() {
        showUntil = 0L
        shownSince = 0L
        trackedEntity = null
        activeIcon = TargetIconRegistry.NONE
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        val target = event.entity

        if (target !is LivingEntity || !target.isAlive) return@handler
        if (onlyKillAura && !ModuleKillAura.running) return@handler

        val now = System.currentTimeMillis()
        val isNewTarget = now >= showUntil || trackedEntity !== target

        val selectedIcon = if (isNewTarget) {
            if (randomIcon) {
                TargetIconRegistry.entries.filter { it.texture != null }.randomOrNull() ?: icon
            } else {
                icon
            }
        } else {
            activeIcon
        }

        if (selectedIcon.texture == null) return@handler

        if (now >= showUntil) {
            shownSince = now
        }

        trackedEntity = target
        activeIcon = selectedIcon
        showUntil = now + DISPLAY_DURATION
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
        val texture = activeIcon.texture ?: return@handler
        val now = System.currentTimeMillis()

        if (now >= showUntil) {
            showUntil = 0L
            shownSince = 0L
            trackedEntity = null
            activeIcon = TargetIconRegistry.NONE
            return@handler
        }

        val target = trackedEntity
        if (target == null || !target.isAlive || target.isRemoved) {
            showUntil = 0L
            shownSince = 0L
            trackedEntity = null
            activeIcon = TargetIconRegistry.NONE
            return@handler
        }

        val worldPos = target.interpolateCurrentPosition(event.tickDelta)
            .add(0.0, target.bbHeight.toDouble() * heightFraction, 0.0)
        val screenPos = WorldToScreen.calculateScreenPos(worldPos) ?: return@handler

        val size = iconSize.toFloat()
        val x0 = screenPos.x + offsetX - size * 0.5f
        val y0 = screenPos.y + offsetY - size * 0.5f

        event.context.drawTexQuad(
            textureSetup = texture.textureSetup,
            x0 = x0,
            y0 = y0,
            x1 = x0 + size,
            y1 = y0 + size,
            u1 = 0f,
            v1 = 0f,
            u2 = 1f,
            v2 = 1f,
            argb = Color4b(255, 255, 255, 255).argb,
            pipeline = ClientRenderPipelines.GUI.TexQuadNoCull,
        )
    }

}
