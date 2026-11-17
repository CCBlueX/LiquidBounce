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
package net.ccbluex.liquidbounce.features.module.modules.render.esp

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.Esp2DMode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspBoxMode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspLegacy2DMode
import net.ccbluex.liquidbounce.render.GenericDistanceHSBColorMode
import net.ccbluex.liquidbounce.render.GenericEntityHealthColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.entity.vehicle.VehicleEntity

/**
 * ESP module
 *
 * Allows you to see targets through walls.
 */
object ModuleESP : ClientModule("ESP", ModuleCategories.RENDER) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.esp"

    val modes = choices("Mode", EspGlowMode, arrayOf(
        EspBoxMode,
        Esp2DMode,
        EspLegacy2DMode,
//        EspOutlineMode,
        EspGlowMode
    ))

    private val colorModes = choices("ColorMode", 0) {
        arrayOf(
            GenericDistanceHSBColorMode.entity(it),
            GenericEntityHealthColorMode(it),
            GenericStaticColorMode(it, Color4b.WHITE.with(a = 100)),
            GenericRainbowColorMode(it)
        )
    }
    private val invisibleColor by color("Invisible", Color4b.ORANGE)
    private val friendColor by color("Friends", Color4b.GREEN)

    internal val maximumDistance by float("MaximumDistance", 128F, 1F..512F)

    private val objects by multiEnumChoice<EspObject>("Objects")
    private val objectColor by color("ObjectColor", Color4b.WHITE.with(a = 100))

    /**
     * The [RenderedEntities] and the selected [objects], refreshed together.
     */
    val targets: List<Entity>
        field = ReferenceArrayList<Entity>()

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        RenderedEntities.onUpdated(::refreshTargets)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        targets.clear()
    }

    private fun refreshTargets() {
        targets.clear()
        targets.addAll(RenderedEntities)

        if (objects.isEmpty()) {
            return
        }

        for (entity in mc.level?.entitiesForRendering() ?: return) {
            if (isObject(entity)) {
                targets += entity
            }
        }
    }

    /**
     * Whether [entity] is one of the selected [objects]. The player's own vehicle never is: it surrounds the camera.
     */
    fun isObject(entity: Entity) = entity !== mc.player?.vehicle && objects.any { it.matches(entity) }

    fun shouldShow(entity: Entity) = entity.shouldBeShown() || isObject(entity)

    fun getColor(entity: Entity): Color4b {
        if (entity !is LivingEntity) {
            return objectColor
        }

        if (entity.hurtTime > 0) {
            return Color4b.RED
        }

        if (entity.isInvisible) {
            return invisibleColor
        }

        if (entity is Player) {
            if (FriendManager.isFriend(entity) && friendColor.a > 0) {
                return friendColor
            }

            EntityTaggingManager.getTag(entity).color?.let { return it }
        }

        return colorModes.activeMode.getColor(entity)
    }

    /**
     * Check if the entity requires true sight to be shown with the current ESP mode
     */
    fun requiresTrueSight(entity: LivingEntity) =
        modes.activeMode.requiresTrueSight && entity.shouldBeShown()

    private enum class EspObject(override val tag: String, val matches: (Entity) -> Boolean) : Tagged {
        END_CRYSTALS("EndCrystals", { it is EndCrystal }),
        PROJECTILES("Projectiles", { it is Projectile }),
        VEHICLES("Vehicles", { it is VehicleEntity }),
    }

}
