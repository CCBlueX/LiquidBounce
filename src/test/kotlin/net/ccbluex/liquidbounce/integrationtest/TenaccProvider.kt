package net.ccbluex.liquidbounce.integrationtest

import net.ccbluex.liquidbounce.invitro.TestInventoryBlockManagement
import net.ccbluex.liquidbounce.invitro.TestCleanupPlan
import net.ccbluex.tenacc.api.runner.*
import net.ccbluex.tenacc.impl.TestIdentifier
import net.ccbluex.tenacc.utils.TestErrorFormatter
import net.ccbluex.tenacc.utils.chat
import net.minecraft.server.network.ServerPlayerEntity

class TenaccProvider: TACCTestProvider {
    override val headlessMode: Boolean
        get() = System.getenv("TENACC_HEADLESS") != null

    override val structureTemplateBasePath: String
        get() = "/"

    @Suppress("EmptyFunctionBlock")
    override fun init(scheduler: TACCTestScheduler?) {
        if (headlessMode && scheduler != null) {
            scheduler.enqueueTests(
                TestScheduleRequest(TestIdentifier("TestInvCleaner", "testLoopRegressions")),
                TestScheduleRequest(TestIdentifier("TestScaffold", "testRotationBottleneck")),
            )
        }
    }

    override fun onTestFail(player: ServerPlayerEntity, schedulerInfo: ScheduledTest, error: Throwable) {
        player.chat("§cTest §l'${schedulerInfo.fn.identifier}' " +
            "(${schedulerInfo.mirrorType}/${schedulerInfo.rotationType})" +
            "§r§c failed: ${TestErrorFormatter.formatError(error, schedulerInfo.fn)}")
    }

    override fun onTestPass(player: ServerPlayerEntity, schedulerInfo: ScheduledTest) {
        player.chat("§aTest §l'${schedulerInfo.fn.identifier}' " +
            "(${schedulerInfo.mirrorType}/${schedulerInfo.rotationType})§r§a passed.")
    }

    override fun onTestQueueFinish(player: ServerPlayerEntity) {
        player.chat("§bTest queue finished.")
    }

    override fun registerTests(registry: TACCTestRegistry) {
        registry.registerTestClass(TestScaffold::class)
        registry.registerTestClass(TestInvCleaner::class)
        registry.registerTestClass(TestInventoryBlockManagement::class)
        registry.registerTestClass(TestCleanupPlan::class)
    }
}
