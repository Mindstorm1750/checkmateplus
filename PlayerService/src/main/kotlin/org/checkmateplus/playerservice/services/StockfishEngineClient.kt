package org.checkmateplus.playerservice.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.checkmateplus.stockfishengine.proto.StockfishEngineGrpc
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class StockfishEngineClient(
    @Value("\${stockfishengine.host}") private val host: String,
    @Value("\${stockfishengine.port}") private val port: Int,
    @Value("\${analysis.deadline-seconds:600}") private val deadlineSeconds: Long,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var channel: ManagedChannel
    private lateinit var stub: StockfishEngineGrpc.StockfishEngineBlockingStub

    @PostConstruct
    fun initialise() {
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext().build()
        stub = StockfishEngineGrpc.newBlockingStub(channel)
        logger.info("StockfishEngineClient connected to $host:$port")
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    suspend fun analyseMatch(
        request: StockfishEngineProto.AnalyseMatchRequest
    ): StockfishEngineProto.AnalyseMatchResponse = withContext(Dispatchers.IO) {
        stub.withDeadlineAfter(deadlineSeconds, TimeUnit.SECONDS).analyseMatch(request)
    }
}
