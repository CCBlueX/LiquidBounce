package net.ccbluex.liquidbounce.utils

import com.google.common.util.concurrent.ThreadFactoryBuilder
import net.ccbluex.liquidbounce.LiquidBounce
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

fun runAsync(commandBlock: () -> Unit) = AsyncUtils.workers.execute(commandBlock)

fun runAsync(command: Runnable) = AsyncUtils.workers.execute(command)

fun runSync(commandBlock: () -> Unit) = LiquidBounce.wrapper.minecraft.addScheduledTask(commandBlock)

fun <T> supplyAsync(supplierBlock: () -> T): Future<T> = AsyncUtils.workers.submit(supplierBlock)

// delayMillis: Long, task: () -> Unit -> WorkerUtils.scheduledWorkers.schedule(task, delayMillis, TimeUnit.MILLISECONDS)

fun <T> runAsyncDelayed(delayInMillis: Long, commandBlock: () -> T): ScheduledFuture<T> = AsyncUtils.scheduledWorkers.schedule(commandBlock, delayInMillis, TimeUnit.MILLISECONDS)

