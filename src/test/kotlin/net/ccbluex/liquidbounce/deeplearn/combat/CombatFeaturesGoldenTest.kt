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
package net.ccbluex.liquidbounce.deeplearn.combat

import net.minecraft.world.phys.Vec3
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The bundled models were trained on these exact features. Regenerate with `LB_GOLDEN_UPDATE=true` only together
 * with a new [CombatFeatures.VERSION] and new models.
 */
class CombatFeaturesGoldenTest {
    @Test
    fun `features and decisions match the version the bundled models were trained on`() {
        assertEquals(4, CombatFeatures.VERSION)
        assertEquals(109, CombatFeatures.SIZE)
        val actual = vectors()
        if (System.getenv("LB_GOLDEN_UPDATE") == "true") {
            Files.writeString(Path.of("src/test/resources/net/ccbluex/liquidbounce/deeplearn/combat", GOLDEN),
                actual.entries.joinToString("\n", postfix = "\n") { (label, values) ->
                    "$label " + values.joinToString(" ") { Integer.toHexString(it.toRawBits()) }
                })
        }
        val expected = assertNotNull(javaClass.getResourceAsStream(GOLDEN)).bufferedReader().readLines()
            .filter { it.isNotBlank() }
            .associate { line ->
                val parts = line.split(' ')
                parts[0] to parts.drop(1).map { Float.fromBits(Integer.parseUnsignedInt(it, 16)) }
            }
        assertEquals(expected.keys, actual.keys)
        for ((label, values) in actual) {
            val golden = expected.getValue(label)
            assertEquals(golden.size, values.size, label)
            values.forEachIndexed { index, value ->
                assertTrue(abs(value - golden[index]) <= TOLERANCE, "$label[$index]: $value, expected ${golden[index]}")
            }
        }
    }

    private fun vectors() = features() + decisions()

    private fun features() = buildMap {
        for (source in CombatSource.entries) {
            for (style in CombatStyle.entries) {
                val timeline = timeline(source, style)
                for ((lead, tick) in cases(source)) {
                    val out = FloatArray(CombatFeatures.SIZE)
                    CombatFeatures.write(timeline, CombatPerception.of(timeline).lead(lead),
                        CombatView.recorded(timeline), CombatFeatures.recordedAttacks(timeline), tick, out)
                    put("$source/$style/lead$lead/t$tick", out.toList())
                }
            }
        }
    }

    /** Live play is first person with a lead; recordings are read without one. */
    private fun cases(source: CombatSource) = if (source == CombatSource.FIRST_PERSON) {
        listOf(0 to CombatFeatures.HISTORY, 0 to TICKS - 1, 2 to TICKS - 1)
    } else {
        listOf(0 to TICKS - 1)
    }

    private fun decisions(): Map<String, List<Float>> {
        val random = Random(5)
        return (0 until 8).associate { index ->
            val output = FloatArray(CombatOutputs.SIZE) { random.nextDouble(-2.0, 2.0).toFloat() }
            val decision = CombatOutputs.decide(output, index % 3 == 0, 0.5f, 40f, random)
            "decide$index" to listOf(decision.yaw, decision.pitch, if (decision.clamped) 1f else 0f)
        }
    }

    private fun timeline(source: CombatSource, style: CombatStyle): CombatTimeline {
        val random = Random(source.ordinal * 2 + style.ordinal + 1)
        val ping = if (source == CombatSource.FIRST_PERSON) -1 else 75
        val builder = CombatTimelineBuilder(source, style, 767, 1, 2, ping, -1)
        val self = Fighter(Vec3(10.0, 64.0, -4.0), random)
        val target = Fighter(Vec3(12.0, 64.0, -1.5), random)
        for (tick in 0 until TICKS) {
            val context = CombatSelfContext(
                lineOfSight = random.nextInt(5) != 0,
                probes = random.nextInt(1 shl 16),
                nearestOther = if (random.nextBoolean()) random.nextInt(255) else CombatTrack.UNKNOWN,
                othersSwingNear = random.nextInt(6) == 0,
                input = if (random.nextInt(8) == 0) CombatTrack.UNKNOWN else random.nextInt(128),
                clicks = if (random.nextInt(4) == 0) 1 else 0,
            )
            builder.add(self.next(local = true), target.next(local = false), context)
        }
        return builder.build(1).also {
            if (source != CombatSource.FIRST_PERSON) {
                it.skill = 0.45f
            }
        }
    }

    private class Fighter(private var position: Vec3, private val random: Random) {
        private var yaw = random.nextDouble(-180.0, 180.0).toFloat()
        private var pitch = 0f
        private var entityId = random.nextInt(1000)

        fun next(local: Boolean): CombatFrame {
            position = position.add(random.nextDouble(-0.3, 0.3), if (random.nextInt(6) == 0) 0.42 else 0.0,
                random.nextDouble(-0.3, 0.3))
            yaw += random.nextDouble(-25.0, 25.0).toFloat()
            pitch = (pitch + random.nextDouble(-8.0, 8.0).toFloat()).coerceIn(-90f, 90f)
            return CombatFrame(
                entityId = entityId,
                position = position,
                yaw = yaw,
                pitch = pitch,
                state = random.nextInt(1 shl 10),
                events = random.nextInt(1 shl 12),
                hurtTime = random.nextInt(11),
                health = random.nextInt(256),
                attackDelay = if (random.nextInt(5) == 0) CombatTrack.UNKNOWN else random.nextInt(60, 130),
                attackStrength = if (!local || random.nextInt(5) == 0) CombatTrack.UNKNOWN else random.nextInt(101),
                item = CombatItem.entries[random.nextInt(CombatItem.entries.size)],
                width = 0.6f,
                height = if (random.nextInt(10) == 0) 1.5f else 1.8f,
                eyeHeight = 1.62f,
            )
        }
    }

    private companion object {
        const val GOLDEN = "combat-features-v4.golden"
        const val TICKS = 60
        const val TOLERANCE = 1e-5f
    }
}
