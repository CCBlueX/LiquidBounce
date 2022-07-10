package net.ccbluex.liquidbounce.utils

import com.google.common.util.concurrent.ThreadFactoryBuilder
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import java.util.concurrent.*

object AsyncUtils
{
    @JvmStatic
    val workers: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 30L, TimeUnit.SECONDS, LinkedBlockingQueue(), ThreadFactoryBuilder().setDaemon(true).setNameFormat("LiquidBounce Asynchronous Worker #%d").build())

    @JvmStatic
    val scheduledWorkers: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1, ThreadFactoryBuilder().setDaemon(true).setNameFormat("LiquidBounce Scheduled Asynchronous Worker #%d").build())

    init
    {
        scheduledWorkers.setKeepAliveTime(30L, TimeUnit.SECONDS)
    }
}

// Kotlin style
fun runAsync(commandBlock: () -> Unit) = AsyncUtils.workers.execute(commandBlock)

// Java style
fun runAsync(command: Runnable) = AsyncUtils.workers.execute(command)

fun runSync(commandBlock: () -> Unit) = mc.addScheduledTask(commandBlock)

fun <T> supplyAsync(supplierBlock: () -> T): Future<T> = AsyncUtils.workers.submit(supplierBlock)

fun <T> supplySync(supplierBlock: () -> T): Future<T> = mc.addScheduledTask(supplierBlock)

fun <T> runAsyncDelayed(delayInMillis: Long, commandBlock: () -> T): ScheduledFuture<T> = AsyncUtils.scheduledWorkers.schedule(commandBlock, delayInMillis, TimeUnit.MILLISECONDS)

