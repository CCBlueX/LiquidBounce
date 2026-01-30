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

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.fastutil.filterIsInstanceTo
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.withColor
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.util.Mth
import net.minecraft.world.entity.item.PrimedTnt
import java.text.DecimalFormat
import java.util.function.IntFunction
import kotlin.math.sin

/**
 * TNTTimer module
 *
 * Highlight the active TNTs.
 */
object ModuleTNTTimer : ClientModule("TNTTimer", ModuleCategories.RENDER) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.tntTimer"

    // Glow ESP
    val esp by boolean("ESP", true)

    private object ShowTimer : ToggleableValueGroup(this, "ShowTimer", false) {
        val scale by float("Scale", 1.5F, 0.25F..4F)
        val renderY by float("RenderY", 1.0F, -2.0F..2.0F)
        val ownerName by boolean("OwnerName", true)
        val timeUnit by enumChoice("TimeUnit", TimeUnit.TICKS)

        enum class TimeUnit(override val tag: String): Tagged, IntFunction<String> {
            TICKS("Ticks"),
            SECONDS("Seconds");

            override fun apply(t: Int): String = when (this) {
                TICKS -> t.toString()
                SECONDS -> SECONDS_FORMAT.format(t * 0.05)
            }
        }

        private val SECONDS_FORMAT = DecimalFormat("0.00s")

        @Suppress("unused")
        private val render2DHandler = handler<OverlayRenderEvent> { event ->
            for (tnt in tntEntities) {
                val pos = tnt.box.center.add(0.0, renderY.toDouble(), 0.0)

                val screenPos = WorldToScreen.calculateScreenPos(pos) ?: continue

                // Yellow #ffff00 -> Red #ff0000
                val color = Color4b(255, Mth.floor(255F * tnt.fuse / DEFAULT_FUSE).coerceAtMost(255), 0)

                val text = "".asText()
                    .append(timeUnit.apply(tnt.fuse).asText().withColor(color.toTextColor()))

                if (ownerName) {
                    tnt.owner?.name?.let {
                        text.append(" (").append(name).append(")")
                    }
                }

                event.context.drawItemStackList(emptyList())
                    .centerX(screenPos.x)
                    .centerY(screenPos.y)
                    .title(text)
                    .scale(scale)
                    .draw()
            }
        }
    }

    init {
        tree(ShowTimer)
    }

    private const val DEFAULT_FUSE = 80

    /**
     * Cycle light periodically according to the remaining time (`fuse`). The less time left, the faster the cycle.
     */
    fun getTntColor(fuse: Int): Color4b {
        val red = Mth.floor(255.0 * (1.0 + 0.5 * sin(2400.0 / (12 + fuse)))).coerceIn(0, 255)
        return Color4b(red, 0, 0)
    }

    private val tntEntities by computedOn<GameTickEvent, MutableSet<PrimedTnt>>(ReferenceOpenHashSet()) { _, set ->
        set.clear()
        world.entitiesForRendering().filterIsInstanceTo(set) { it.fuse > 0 }
        set
    }

    override fun onDisabled() {
        tntEntities.clear()
        super.onDisabled()
    }

}
