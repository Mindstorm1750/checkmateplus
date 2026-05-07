package org.checkmateplus.playerservice.controllers

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.chessEngine.FenToBoard
import org.checkmateplus.chessLibrary.chessEngine.PieceType
import org.checkmateplus.chessLibrary.chessEngine.SanFormatter.toSan
import org.checkmateplus.chessLibrary.chessEngine.toAlgebraic
import org.checkmateplus.chessLibrary.playerModels.history.Turn
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.entitys.players.AnonymousPlayerEntity
import org.checkmateplus.playerservice.entitys.players.BotPlayerEntity
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.checkmateplus.playerservice.models.MatchMoveHistoryView
import org.checkmateplus.playerservice.models.MoveHistoryView
import org.checkmateplus.playerservice.models.SpectatePlayer
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MatchHistoryGraphQLController(
    private val matchRecordRepository: MatchRecordRepository,
) {
    private val turnsSerializer = ListSerializer(Turn.serializer())

    @QueryMapping
    fun matchMoveHistory(@Argument matchId: String): MatchMoveHistoryView? {
        val entity = matchRecordRepository.findById(matchId).orElse(null) ?: return null
        return entity.toMoveHistoryView()
    }

    private fun MatchRecordEntity.toMoveHistoryView(): MatchMoveHistoryView {
        val turns = Json.decodeFromString(turnsSerializer, historyJson)
        val whitePlayerId = whitePlayer?.playerId
        val moves = turns.map { it.toView(whitePlayerId) }
        return MatchMoveHistoryView(
            matchId = matchId,
            whitePlayer = whitePlayer?.toSpectatePlayer(),
            blackPlayer = blackPlayer.toSpectatePlayer(),
            timeControl = timeControl,
            matchType = matchType.name,
            outcome = matchOutcome.name,
            finalFen = board.toFen(),
            moves = moves,
            createdAtEpochMs = createdAtEpochMs,
            gameDurationMs = gameDurationMs,
        )
    }

    private fun Turn.toView(whitePlayerId: String?): MoveHistoryView {
        val from = move.from.toAlgebraic()
        val to = move.to.toAlgebraic()
        val colour = if (player.playerId == whitePlayerId) "WHITE" else "BLACK"
        val hotfixBoard = FenToBoard(fen)
        return MoveHistoryView(
            san = toSan(hotfixBoard, move, hotfixBoard.applyMove(move)),
            from = from,
            to = to,
            fenAfter = fen,
            colour = colour,
            playerId = player.playerId,
        )
    }

    private fun PlayerEntity.toSpectatePlayer(): SpectatePlayer {
        val type = when (this) {
            is BotPlayerEntity -> "BOT"
            is AnonymousPlayerEntity -> "ANONYMOUS"
            is RegisteredPlayerEntity -> "REGISTERED"
            else -> "ANONYMOUS"
        }
        return SpectatePlayer(
            playerId = playerId,
            displayName = displayName,
            rating = skillRating ?: 0,
            type = type,
        )
    }

    private fun promoChar(p: PieceType): String = when (p) {
        PieceType.QUEEN -> "q"
        PieceType.ROOK -> "r"
        PieceType.BISHOP -> "b"
        PieceType.KNIGHT -> "n"
        else -> ""
    }
}
