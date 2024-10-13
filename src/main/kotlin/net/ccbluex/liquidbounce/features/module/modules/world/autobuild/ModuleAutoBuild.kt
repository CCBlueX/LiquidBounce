/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.autobuild

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object ModuleAutoBuild : Module("AutoBuild", Category.WORLD, aliases = arrayOf("Platform", "AutoPortal")) {

    private val mode = choices("Mode", PortalMode, arrayOf(PortalMode, PlatformMode))
    val placer = tree(BlockPlacer("Placing", this, Priority.NOT_IMPORTANT) { mode.activeChoice.getSlot() })

    init {
        mode.onChanged { enabled = false }
    }

    override fun enable() {
        mode.activeChoice.enabled()
    }

    override fun disable() {
        placer.disable()
        mode.activeChoice.disabled()
    }

    abstract class AutoBuildMode(name: String) : Choice(name) {

        abstract fun getSlot(): HotbarItemSlot?

        open fun enabled() {}

        open fun disabled() {}

        override val parent: ChoiceConfigurable<*>
            get() = mode

    }

}
