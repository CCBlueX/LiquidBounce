package net.ccbluex.liquidbounce.integrationtest

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.integrationtest.util.loadSettingsFromPath
import net.ccbluex.tenacc.api.TACCTest
import net.ccbluex.tenacc.api.TACCTestClass
import net.ccbluex.tenacc.api.common.TACCSequenceAdapter
import net.ccbluex.tenacc.api.common.TACCTestSequence
import net.ccbluex.tenacc.utils.*

@TACCTestClass("TestInvCleaner")
class TestInvCleaner {

    @TACCTest(name = "testLoopRegressions", scenary = "generic/one_block_platform.nbt", timeout = 1000)
    fun runTest(adapter: TACCSequenceAdapter) {
        adapter.startSequence {
            val cases = arrayOf(
                "/invcleaner/9bb7f967fe02fe08.nbttxt",
                "/invcleaner/93bce905c1b7e016.nbttxt"
            )

            for (case in cases) {
                // Find problems, even if they are rare.
                for (i in 0..10) {
                    server { log("Testing case $case, Iteration $i") }

                    testLoop(case)
                }
            }
        }
    }

    private suspend fun TACCTestSequence.testLoop(fileName: String) {
        client {
            ModuleInventoryCleaner.enabled = false

            resetSettings()
        }

        server {
            resetStandardConditions()

            player.loadInventory(fileName)
            player.sendInventoryUpdates()

            val pos = getMarkerPos("center")

            player.teleport(
                player.serverWorld,
                pos.x.toDouble() + 0.5,
                pos.y.toDouble(),
                pos.z.toDouble() + 0.5,
                0.0F,
                20F
            )
        }

        // Don't turn the inv cleaner on until the inventory is sent
        sync()

        client {
            ModuleInventoryCleaner.enabled = true
        }

        sync()

        serverSequence {
    //                waitUntilOrFail(20, "InvCleaner is cleaning too long.") {
    //
    //                }

            var changeCount = 0
            var ticksWithoutChange = 0

            for (i in 0..25 + 3) {
                if (changeCount != it.player.inventory.changeCount) {
                    ticksWithoutChange = 0
                }

                if (ticksWithoutChange >= 3) {
                    return@serverSequence
                }

                changeCount = it.player.inventory.changeCount

                ticksWithoutChange++

                waitTicks(1)
            }

            failTest("InvCleaner did not come to rest after the expected time.")
        }

        sync()

        client {
            ModuleInventoryCleaner.enabled = false
        }
    }

    fun resetSettings() {
        loadSettingsFromPath(ModuleInventoryCleaner, "/invcleaner/invcleaner_baseline.json")
    }


}
