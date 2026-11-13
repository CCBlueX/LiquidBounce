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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.MapFormat.Block
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.MapFormat.ChunkBlocks
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MapFormatTest {

    private val palette = MapFormat.Palette().apply {
        id("minecraft:stone") { Block("minecraft:stone", 1, colored = true, solid = true, false, false, false) }
        id("minecraft:water") { Block("minecraft:water", 2, colored = true, solid = false, true, false, false) }
        id("minecraft:glass") { Block("minecraft:glass", 0, colored = false, solid = true, false, false, false) }
        id("minecraft:chest") { Block("minecraft:chest", 3, colored = true, solid = true, false, false, true) }
    }

    private val stone = 1
    private val water = 2
    private val glass = 3
    private val chest = 4

    /** 300 blocks high from y=10, so one column needs a run longer than [MapFormat.MAX_RUN]. */
    private fun chunk(): ChunkBlocks {
        val height = 300
        val ids = CharArray(height * MapFormat.COLUMNS)
        fun set(column: Int, y: Int, id: Int) {
            ids[(y - 10) * MapFormat.COLUMNS + column] = id.toChar()
        }
        // Column 0: stone 12..14, water 15.
        for (y in 12..14) set(0, y, stone)
        set(0, 15, water)
        // Column 17 (lx=1, lz=1): stone 12, glass 13 on top, chest 11 below.
        set(17, 11, chest)
        set(17, 12, stone)
        set(17, 13, glass)
        // Column 255: stone from 11 up to 309.
        for (y in 11..309) set(255, y, stone)
        return ChunkBlocks(10, height, ids)
    }

    @Test
    fun `columns hold top, surface and standing block`() {
        val bytes = MapFormat.columns(chunk(), palette.blocks)
        assertEquals(MapFormat.COLUMNS * MapFormat.COLUMN_BYTES, bytes.size)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        fun record(column: Int): List<Int> {
            buffer.position(column * MapFormat.COLUMN_BYTES)
            return listOf(
                buffer.short.toInt(), buffer.short.toInt(), buffer.short.toInt(),
                buffer.char.code, buffer.char.code, buffer.get().toInt(),
            )
        }

        assertEquals(listOf(15, 15, 14, water, stone, MapFormat.FLAG_WATER), record(0))
        assertEquals(
            listOf(13, 12, 13, stone, glass, MapFormat.FLAG_TRANSLUCENT or MapFormat.FLAG_BLOCK_ENTITY),
            record(17),
        )
        val none = MapFormat.NONE
        assertEquals(listOf(none, none, none, 0, 0, 0), record(1))
    }

    @Test
    fun `world runs start at the lowest block and split long runs`() {
        val buffer = ByteBuffer.wrap(MapFormat.rle(chunk())).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(11, buffer.short.toInt())

        val columns = List(MapFormat.COLUMNS) {
            List(buffer.char.code) { (buffer.get().toInt() and 0xFF) to buffer.char.code }
        }
        assertEquals(false, buffer.hasRemaining())
        assertEquals(listOf(1 to 0, 3 to stone, 1 to water), columns[0])
        assertEquals(listOf(1 to chest, 1 to stone, 1 to glass), columns[17])
        assertEquals(listOf(255 to stone, 44 to stone), columns[255])
        assertEquals(emptyList(), columns[1])
    }

    @Test
    fun `empty chunk has no base and no runs`() {
        val bytes = MapFormat.rle(ChunkBlocks(0, 16, CharArray(16 * MapFormat.COLUMNS)))
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(MapFormat.NONE, buffer.short.toInt())
        assertEquals(2 + 2 * MapFormat.COLUMNS, bytes.size)
    }

    @Test
    fun `gzip round trips`() {
        val bytes = MapFormat.columns(chunk(), palette.blocks)
        val unpacked = GZIPInputStream(ByteArrayInputStream(MapFormat.gzip(bytes, 9))).readBytes()
        assertContentEquals(bytes, unpacked)
    }

    @Test
    fun `palette keeps ids by name`() {
        assertEquals(stone, palette.id("minecraft:stone") { error("known") })
        assertEquals("minecraft:air", palette.blocks[0].name)
    }

    @Test
    fun `fixed decimals`() {
        fun fixed(value: Double, decimals: Int) = StringBuilder().also { MapFormat.appendFixed(it, value, decimals) }
            .toString()
        assertEquals("-41.23", fixed(-41.234, 2))
        assertEquals("0.05", fixed(0.049, 2))
        assertEquals("0.0", fixed(-0.01, 1))
        assertEquals("70", fixed(70.0, 0))
        assertEquals("", fixed(Double.NaN, 2))
    }

    @Test
    fun `json values`() {
        val json = StringBuilder().also {
            MapFormat.appendJson(
                it,
                mapOf(
                    "t" to 3, "pos" to doubleArrayOf(-41.0, 70.456, -2.5), "block" to intArrayOf(1, -2),
                    "text" to "a \"b\"\n\u0001", "ok" to true, "none" to null, "nan" to Double.NaN,
                    "list" to listOf("x", 0.1),
                ),
            )
        }.toString()
        assertEquals(
            """{"t":3,"pos":[-41,70.46,-2.5],"block":[1,-2],"text":"a \"b\"\n\u0001","ok":true,""" +
                """"none":null,"nan":null,"list":["x",0.1]}""",
            json,
        )
    }

}
