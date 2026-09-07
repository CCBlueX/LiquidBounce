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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientAppearanceSubcommand {
    fun CmdLiteralScope.appearance() {
        literal("appearance") {
            literal("show") {
                exec {
                    if (!HideAppearance.isHidingNow) {
                        chat(regular(t("appearance.show.alreadyShowingAppearance")))
                        return@exec 1
                    }

                    chat(regular(t("appearance.show.showingAppearance")))
                    HideAppearance.isHidingNow = false
                    1
                }
            }
            literal("hide") {
                exec {
                    if (HideAppearance.isHidingNow) {
                        chat(regular(t("appearance.hide.alreadyHidingAppearance")))
                        return@exec 1
                    }

                    chat(regular(t("appearance.hide.hidingAppearance")))
                    HideAppearance.isHidingNow = true
                    1
                }
            }
        }
    }
}
