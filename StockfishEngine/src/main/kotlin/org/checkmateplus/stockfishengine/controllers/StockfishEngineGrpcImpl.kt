package org.checkmateplus.stockfishengine.controllers

import io.grpc.stub.StreamObserver
import org.checkmateplus.stockfishengine.proto.StockfishEngineGrpc
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto
import org.checkmateplus.stockfishengine.services.MatchAnalyser
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class StockfishEngineGrpcImpl(
    private val matchAnalyser: MatchAnalyser,
) : StockfishEngineGrpc.StockfishEngineImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun analyseMatch(
        request: StockfishEngineProto.AnalyseMatchRequest,
        responseObserver: StreamObserver<StockfishEngineProto.AnalyseMatchResponse>
    ) {
        logger.info("AnalyseMatch start matchId={} plies={}", request.matchId, request.uciMovesCount)
        val response = matchAnalyser.analyse(request)
        responseObserver.onNext(response)
        responseObserver.onCompleted()
        logger.info("AnalyseMatch done matchId={} success={}", request.matchId, response.success)
    }
}
