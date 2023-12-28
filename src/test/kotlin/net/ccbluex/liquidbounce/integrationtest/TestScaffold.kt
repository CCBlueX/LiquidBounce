package net.ccbluex.liquidbounce.integrationtest

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.integrationtest.util.isStandingOn
import net.ccbluex.tenacc.api.TACCTest
import net.ccbluex.tenacc.api.TACCTestClass
import net.ccbluex.tenacc.api.client.InputKey
import net.ccbluex.tenacc.api.common.TACCSequenceAdapter
import net.ccbluex.tenacc.api.common.TACCTestSequence
import net.ccbluex.tenacc.utils.Rotation
import net.ccbluex.tenacc.utils.lookDirection
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import org.joml.Vector3f
import java.io.InputStreamReader
import java.io.StringReader

@TACCTestClass("TestScaffold")
class TestScaffold {

    fun loadSettingsFromPath(module: Module, path: String) {
        ConfigSystem.deserializeConfigurable(
            module,
            InputStreamReader(TestScaffold::class.java.getResourceAsStream(path) ?: throw IllegalArgumentException("Path $path was not found"))
        )
    }

    fun loadSettingsFromString(module: Module, vararg values: String) {
        ConfigSystem.deserializeConfigurable(
            module,
            StringReader(settingsContainer(module.name, *values))
        )
    }

    fun settingsContainer(name: String, vararg values: String): String {
        val valuesFormatted = values.joinToString(",") { it }

        return "{\"name\": \"${name}\", \"value\": [$valuesFormatted]}"
    }

    fun <T> rangeSetting(name: String, from: T, to: T): String {
        return """{"name": "$name", "value": {"from": $from,"to": $to}}"""
    }

    fun stringSetting(name: String, value: String): String {
        return """{"name": "$name", "value": "$value"}""".trimIndent()
    }

    fun choiceSetModeSetting(name: String, value: String): String {
        return """{"name": "$name", "active": "$value",value:[]}""".trimIndent()
    }

    fun resetSettings() {
        loadSettingsFromPath(ModuleScaffold, "/scaffold/scaffold_baseline.json")
    }

    @TACCTest(name = "testRotationBottleneck", scenary = "scaffold/scaffold_underground_straight.nbt", timeout = 550)
    fun runTest(adapter: TACCSequenceAdapter) {
        adapter.startSequence {
            val startPositions = server { getMarkerPositions("s_interl_blocks") }

            server {
                player.inventory.setStack(0, ItemStack(Items.STONE, 64))
            }

            loopByServer(startPositions) { startPositionBox ->
                server {
                    resetScenery()

                    val pos = openBox(startPositionBox)

                    log("Testing sign $pos")

                    player.teleport(
                        player.serverWorld,
                        pos.x.toDouble() + 0.5,
                        pos.y.toDouble(),
                        pos.z.toDouble() + 0.5,
                        0.0F,
                        20F
                    )
                }

                waitTicks(2)

                sync()

                runLoop()
            }
        }
    }

    private suspend fun TACCTestSequence.runLoop() {
        client {
            resetSettings()

            loadSettingsFromString(
                ModuleScaffold,
                rangeSetting("Delay", 6, 6),
                stringSetting("RotationMode", "Center"),
                choiceSetModeSetting("SafeWalk", "Safe"),
                settingsContainer(
                    name = "Rotations",
                    rangeSetting("TurnSpeed", 30.0, 30.0)
                )
            )

            ModuleScaffold.enabled = false
            ModuleScaffold.enabled = true

            player.inventory.selectedSlot = 0

            player.lookDirection(
                Rotation.fromDirection(
                    Vector3f(0.0F, 0.0F, -1.0F),
                    templateInfo.transformation
                ))

            clearInputs()
            sendInputs(InputKey.KEY_BACKWARDS, nTicks = 1)
            sendInputs(InputKey.KEY_FORWARDS)
        }

        serverSequence { server ->
            val goalBlocks = server.getMarkerPositions("end")

            // It has to place 11 blocks with 8 ticks delay. additionally it has 25 ticks for initial rotation
            waitUntilOrFail(6 * 11 + 25 , "Failed to reach goal in time") {
                goalBlocks.any { server.player.isStandingOn(it) }
            }

            server.log("Player reached goal")

            permitFencePassage(1)
        }

        waitForFencePassage(1)
    }

}
