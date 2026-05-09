package org.checkmateplus.playerservice.services

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Component
class AnalysisQueueConsumer(
    private val analysisQueue: AnalysisQueue,
    private val matchAnalysisWorker: MatchAnalysisWorker,
    @Value("\${analysis.enabled:true}") private val enabled: Boolean,
    @Value("\${analysis.queue.poll-seconds:10}") private val pollSeconds: Long,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("analysis-queue-consumer")
    )

    @PostConstruct
    fun start() {
        if (!enabled) {
            logger.info("Analysis disabled — queue consumer not started")
            return
        }
        scope.launch { loop() }
        logger.info("AnalysisQueueConsumer started (poll={}s)", pollSeconds)
    }

    @PreDestroy
    fun stop() {
        runBlocking {
            withTimeoutOrNull(5_000.milliseconds) { scope.coroutineContext.job.cancelAndJoin() }
        }
    }

    private suspend fun loop() {
        val pollTimeout = Duration.ofSeconds(pollSeconds)
        while (currentCoroutineContext().isActive) {
            try {
                val matchId = analysisQueue.take(pollTimeout) ?: continue
                logger.info("Picked matchId={} from queue", matchId)
                matchAnalysisWorker.run(matchId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Queue consumer iteration failed: {}", e.message, e)
                delay(1_000.milliseconds)
            }
        }
        logger.info("AnalysisQueueConsumer loop exited")
    }
}
