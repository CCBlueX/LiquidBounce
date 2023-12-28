package net.ccbluex.liquidbounce.integrationtest

import net.ccbluex.tenacc.api.runner.ScheduledTest
import net.ccbluex.tenacc.api.runner.TACCTestProvider
import net.ccbluex.tenacc.api.runner.TACCTestRegistry
import net.ccbluex.tenacc.api.runner.TACCTestScheduler
import net.ccbluex.tenacc.utils.TestErrorFormatter
import net.ccbluex.tenacc.utils.chat
import net.minecraft.server.network.ServerPlayerEntity

class TenaccProvider: TACCTestProvider {
    var a = 0

    override val structureTemplateBasePath: String
        get() = "C:\\Users\\David\\IdeaProjects\\LiquidBounce\\src\\test\\resources\\"

    override fun init(scheduler: TACCTestScheduler?) {}


    override fun onTestFail(player: ServerPlayerEntity, schedulerInfo: ScheduledTest, error: Throwable) {
        player.chat("§cTest §l'${schedulerInfo.fn.identifier}' " +
            "(${schedulerInfo.mirrorType}/${schedulerInfo.rotationType})§r§c failed: ${TestErrorFormatter.formatError(error, schedulerInfo.fn)}")
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
    }
}
