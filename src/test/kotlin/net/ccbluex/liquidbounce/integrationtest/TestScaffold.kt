package net.ccbluex.liquidbounce.integrationtest

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.integrationtest.util.applySettings
import net.ccbluex.liquidbounce.integrationtest.util.isStandingOn
import net.ccbluex.liquidbounce.integrationtest.util.loadSettingsFromPath
import net.ccbluex.tenacc.api.TACCTest
import net.ccbluex.tenacc.api.TACCTestClass
import net.ccbluex.tenacc.api.client.InputKey
import net.ccbluex.tenacc.api.common.TACCSequenceAdapter
import net.ccbluex.tenacc.api.common.TACCTestSequence
import net.ccbluex.tenacc.api.common.TACCTestVariant
import net.ccbluex.tenacc.utils.*
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import org.joml.Vector3f

@TACCTestClass("TestScaffold")
class TestScaffold {

    @TACCTest(name = "testRotationBottleneck", scenary = "scaffold/scaffold_underground_straight.nbt", timeout = 1000)
    fun runTest(adapter: TACCSequenceAdapter) {
        genericTestRotationBottleneck(adapter, diagonal=false)
    }

    private fun genericTestRotationBottleneck(adapter: TACCSequenceAdapter, diagonal: Boolean) {
        fun loadBaseSettings(mode: String) {
            resetSettings()

            applySettings(ModuleScaffold) {
                intRange("Delay", 6..6)
                choice("RotationMode", mode)
                choice("SafeWalk", "Safe")

                configurable("Rotations") {
                    floatRange("TurnSpeed", 30.0F..30.0F)
                    int("TicksUntilReset", 10)
                }
            }
        }

//        val rotationModes = arrayOf("Center", "Stabilized", "NearestRotation", "OnTick")
        val rotationModes = arrayOf("Stabilized")
        val possibleItems = arrayOf(Items.STONE)


        val rotationModeVariants = rotationModes.map {
            TACCTestVariant.of("RotationMode $it") { loadBaseSettings(it) }
        }

        var itemInHand = Items.STONE

        val itemVariants = possibleItems.map {
            TACCTestVariant.of("Item ${it.name.outputString()}") { itemInHand = it }
        }

        val allVariants = TACCTestVariant.combine(rotationModeVariants.toTypedArray(), itemVariants.toTypedArray())

        adapter.startSequence(allVariants) {
            val startPositions = server { getMarkerPositions("start") }

            loopByServer(startPositions) { startPositionBox ->
                client {
                    clearInputs()
                }
                server {
                    resetStandardConditions()

                    player.inventory.setStack(0, ItemStack(itemInHand, 64))

                    player.sendInventoryUpdates()

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

                // Wait for the rotation to reset
                waitTicks(12)

                sync()

                runLoop()
            }
        }
    }

    private suspend fun TACCTestSequence.runLoop() {
        client {
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
            waitUntilOrFail(6 * 11 + 30 , "Failed to reach goal in time") {
                goalBlocks.any { server.player.isStandingOn(it) }
            }

            server.log("Player reached goal")
        }

        sync()
    }

    fun resetSettings() {
        loadSettingsFromPath(ModuleScaffold, "/scaffold/scaffold_baseline.json")
    }


}
