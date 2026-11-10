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
package net.ccbluex.liquidbounce.utils.text

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeathMessageTest {

    private val names = listOf("Enemy", "Enemy_2", "Self")

    private fun read(message: Component) = message.toDeathMessage(names) { it == "Other" }

    private fun read(message: String) = read(Component.literal(message))

    private fun vanilla(key: String, vararg players: String) =
        Component.translatable(key, *players.map { Component.literal(it) }.toTypedArray())

    @Test
    fun `vanilla names victim and killer as arguments`() {
        val kill = vanilla("death.attack.player", "[Red] Enemy", "Self")
        assertEquals(DeathMessage("Enemy", "Self", exact = true), read(kill))

        val shot = vanilla("death.attack.arrow", "Stranger", "Self")
        assertEquals(DeathMessage("Stranger", "Self", exact = true), read(shot))

        assertEquals(DeathMessage("Self", null, exact = true), read(vanilla("death.attack.fall", "Self")))
        assertNull(read(vanilla("multiplayer.player.joined", "Self")))
    }

    @Test
    fun `custom messages start with the victim`() {
        val kill = Component.empty()
            .append(Component.literal("Enemy_2").withStyle(ChatFormatting.RED))
            .append(Component.literal(" was killed by ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("Self").withStyle(ChatFormatting.GREEN))
            .append(Component.literal(". FINAL KILL!"))
        assertEquals(DeathMessage("Enemy_2", "Self", exact = false), read(kill))

        val legacy = "§cEnemy §7was knocked into the void by §aSelf§7."
        assertEquals(DeathMessage("Enemy", "Self", exact = false), read(legacy))
        assertEquals(DeathMessage("Self", "Enemy", exact = false), read("Self was slain by Enemy."))
        assertEquals(DeathMessage("Self", null, exact = false), read("Self fell into the void."))
    }

    @Test
    fun `chat is no death message`() {
        assertNull(read("Enemy: gg Self"))
        assertNull(read("<Enemy> Self was lucky"))
        assertNull(read("Enemy » Self was lucky"))
        assertNull(read("[MVP+] Enemy: Self was lucky"))
        assertNull(read("1st Killer - Self - 5"))
        assertNull(read("Selfish was killed by Enemy."))
    }

    @Test
    fun `custom messages may follow a server prefix`() {
        val kill = DeathMessage("Enemy", "Self", exact = false)
        assertEquals(kill, read("SkyWars ▸ Enemy был убит игроком Self."))
        assertEquals(DeathMessage("Self", "Enemy", exact = false), read("SkyWars ▸ Self был скинут игроком Enemy."))
        assertEquals(DeathMessage("Self", null, exact = false), read("SkyWars ▸ Self упал в бездну."))
        assertEquals(kill, read("Дуэли ▸ Игрок Enemy был убит Self."))
        assertEquals(kill, read("[BedWars] Enemy was killed by Self."))
    }

    @Test
    fun `a prefix with a player is chat`() {
        assertNull(read("[Игрок] Other → Enemy был убит Self"))
        assertNull(read("[7⋆] Stranger: Enemy was killed by Self"))
        assertNull(read("Other was killed by Self with a sword"))
        assertNull(read("Stranger was killed by Self with a sword"))
        assertNull(read("Победа за - Self"))
        assertNull(read("Дуэли ▸ [LEGEND] Other подключился к игре. 2/2"))
    }

}
