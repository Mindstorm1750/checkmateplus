package org.checkmateplus.gameserver.models

import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.checkmateplus.chessLibrary.botEngine.BotModelType
import org.checkmateplus.chessLibrary.botEngine.BotMoveTimeModelType
import org.checkmateplus.chessLibrary.chessEngine.*
import org.checkmateplus.chessLibrary.playerModels.*
import org.checkmateplus.chessLibrary.playerModels.history.MatchOutcome
import org.checkmateplus.chessLibrary.playerModels.history.MatchRecord
import org.checkmateplus.chessLibrary.playerModels.history.Turn
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.playerservice.proto.PlayerServiceProto

fun MatchRecord.toProto(): PlayerServiceProto.MatchRecord {
    val botConfig = botConfiguration
    return PlayerServiceProto.MatchRecord.newBuilder()
        .setBoard(board.toProto())
        .addAllPlayers(players.map { it.toProto() })
        .setMatchId(matchId)
        .setTimeControl(timeControl.toProto())
        .setMatchType(matchType.toProto())
        .setMatchOutcome(matchOutcome.toProto())
        .setWhitePlayer(players.first { it.playerId == whitePlayer.playerId }.toProto())
        .setBlackPlayer(players.first { it.playerId == blackPlayer.playerId }.toProto())
        .setRemainingWhitePlayerTime(remainingWhitePlayerTime)
        .setRemainingBlackPlayerTime(remainingBlackPlayerTime)
        .setMatchStatus(matchStatus.toProto())
        .addAllHistory(history.map { it.toProto() })
        .setMatchCreatedAtEpochMs(matchCreationTime.toEpochMilliseconds())
        .apply { botConfig?.let { bc -> setBotConfiguration(bc.toProto()) } }
        .build()
}

fun Board.toProto(): PlayerServiceProto.Board =
    PlayerServiceProto.Board.newBuilder()
        .addAllSquares(squares.map { it?.toProto() ?: PlayerServiceProto.Piece.getDefaultInstance() })
        .setSideToMove(sideToMove.toProto())
        .setCastlingRights(castlingRights.toProto())
        .apply { setEnPassantSquare(enPassantSquare) }
        .setHalfMoveClock(halfMoveClock)
        .setFullMoveNumber(fullMoveNumber)
        .build()

fun Piece.toProto(): PlayerServiceProto.Piece =
    PlayerServiceProto.Piece.newBuilder()
        .setType(type.toProto())
        .setColour(color.toProto())
        .build()

fun CastlingRights.toProto(): PlayerServiceProto.CastlingRights =
    PlayerServiceProto.CastlingRights.newBuilder()
        .setWhiteKingside(whiteKingSide)
        .setWhiteQueenside(whiteQueenSide)
        .setBlackKingside(blackKingSide)
        .setBlackQueenside(blackQueenSide)
        .build()


fun Move.toProto(): PlayerServiceProto.Move =
    PlayerServiceProto.Move.newBuilder()
        .setFrom(from)
        .setTo(to)
        .apply { promotion?.let { setPromotionPiece(it.toProto()) } }
        .build()


fun Player.toProto(): PlayerServiceProto.Player =
    PlayerServiceProto.Player.newBuilder()
        .setPlayerId(playerId)
        .setDisplayName(displayName)
        .setJoinedQueueAtEpochMs(joinedQueueAt.toEpochMilliseconds())
        .apply { setSkillRating(skillRating) }
        .apply { setRatingDeviation(ratingDeviation) }
        .apply { setVolatility(volatility) }
        .apply {
            when (this@toProto) {
                is Player.Anonymous -> {
                    val prefs = this@toProto.playerPreferences
                    setAnonymous(

                        PlayerServiceProto.AnonymousPlayer.newBuilder()
                            .apply { prefs?.let { setPlayerPreferences(it.toProto()) } }
                            .build()
                    )
                }

                is Player.Bot -> setBot(
                    PlayerServiceProto.BotPlayer.newBuilder()
                        .setDifficulty(difficulty)
                        .setBotConfiguration(botConfiguration.toProto())
                        .build()
                )

                is Player.Registered -> { val prefs = this@toProto.playerPreferences
                    setRegistered(
                        PlayerServiceProto.RegisteredPlayer.newBuilder()
                            .apply { prefs?.let { setPlayerPreferences(it.toProto()) } }
                            .build()
                    )
                }
            }
        }
        .build()

fun PlayerPreferences.toProto(): PlayerServiceProto.PlayerPreferences =
    PlayerServiceProto.PlayerPreferences.newBuilder()
        .setBoardColour(boardColour)
        .setAnimationType(animationType)
        .build()


fun BotConfiguration.toProto(): GameServerProto.BotConfiguration =
    GameServerProto.BotConfiguration.newBuilder()
        .setBotModelType(botModelType.toProto())
        .setBotMoveTimeModelType(botMoveTimeModelType.toProto())
        .build()

fun BotModelType.toProto(): GameServerProto.BotModelType = when (this) {
    BotModelType.RANDOM -> GameServerProto.BotModelType.RANDOM
    BotModelType.MINMAX -> GameServerProto.BotModelType.MINMAX
}

fun BotMoveTimeModelType.toProto(): GameServerProto.BotMoveTimeModelType = when (this) {
    BotMoveTimeModelType.NODELAY -> GameServerProto.BotMoveTimeModelType.NODELAY
    BotMoveTimeModelType.MEDIAN -> GameServerProto.BotMoveTimeModelType.MEDIAN
    BotMoveTimeModelType.REALISTIC -> GameServerProto.BotMoveTimeModelType.REALISTIC
}


fun Turn.toProto(): PlayerServiceProto.Turn =
    PlayerServiceProto.Turn.newBuilder()
        .setPlayer(player.toProto())
        .setMove(move.toProto())
        .setBoard(board.toProto())
        .setFen(fen)
        .setTimeTaken(timeTaken)
        .build()


fun Colour.toProto(): PlayerServiceProto.Colour = when (this) {
    Colour.WHITE -> PlayerServiceProto.Colour.COLOUR_WHITE
    Colour.BLACK -> PlayerServiceProto.Colour.COLOUR_BLACK
}

fun PieceType.toProto(): PlayerServiceProto.PieceType = when (this) {
    PieceType.PAWN -> PlayerServiceProto.PieceType.PIECE_TYPE_PAWN
    PieceType.KNIGHT -> PlayerServiceProto.PieceType.PIECE_TYPE_KNIGHT
    PieceType.BISHOP -> PlayerServiceProto.PieceType.PIECE_TYPE_BISHOP
    PieceType.ROOK -> PlayerServiceProto.PieceType.PIECE_TYPE_ROOK
    PieceType.QUEEN -> PlayerServiceProto.PieceType.PIECE_TYPE_QUEEN
    PieceType.KING -> PlayerServiceProto.PieceType.PIECE_TYPE_KING
}

fun MatchOutcome.toProto(): PlayerServiceProto.MatchOutcome = when (this) {
    MatchOutcome.WHITE_WINS -> PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_WHITE_WINS
    MatchOutcome.BLACK_WINS -> PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_BLACK_WINS
    MatchOutcome.DRAW -> PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_DRAW
    MatchOutcome.ABANDONED -> PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_ABANDONED
}

fun MatchStatus.toProto(): PlayerServiceProto.MatchStatus = when (this) {
    MatchStatus.PENDING -> PlayerServiceProto.MatchStatus.MATCH_STATUS_PENDING
    MatchStatus.ASSIGNED -> PlayerServiceProto.MatchStatus.MATCH_STATUS_ASSIGNED
    MatchStatus.ACTIVE -> PlayerServiceProto.MatchStatus.MATCH_STATUS_ACTIVE
    MatchStatus.COMPLETED -> PlayerServiceProto.MatchStatus.MATCH_STATUS_COMPLETED
    MatchStatus.CANCELLED -> PlayerServiceProto.MatchStatus.MATCH_STATUS_CANCELLED
    MatchStatus.ABANDONED -> PlayerServiceProto.MatchStatus.MATCH_STATUS_ABANDONED
}

fun MatchType.toProto(): PlayerServiceProto.MatchType = when (this) {
    MatchType.CASUAL_PVP -> PlayerServiceProto.MatchType.MATCH_TYPE_CASUAL_PVP
    MatchType.PVE -> PlayerServiceProto.MatchType.MATCH_TYPE_PVE
    MatchType.COMPETITIVE_PVP -> PlayerServiceProto.MatchType.MATCH_TYPE_COMPETITIVE_PVP
}

fun TimeControl.toProto(): PlayerServiceProto.TimeControl = when (this) {
    TimeControl.BULLET_1_0 -> PlayerServiceProto.TimeControl.TIME_CONTROL_BULLET_1_0
    TimeControl.BULLET_2_1 -> PlayerServiceProto.TimeControl.TIME_CONTROL_BULLET_2_1
    TimeControl.BLITZ_3_0 -> PlayerServiceProto.TimeControl.TIME_CONTROL_BLITZ_3_0
    TimeControl.BLITZ_5_0 -> PlayerServiceProto.TimeControl.TIME_CONTROL_BLITZ_5_0
    TimeControl.RAPID_10_0 -> PlayerServiceProto.TimeControl.TIME_CONTROL_RAPID_10_0
    TimeControl.RAPID_15_10 -> PlayerServiceProto.TimeControl.TIME_CONTROL_RAPID_15_10
    TimeControl.CLASSICAL_30_0 -> PlayerServiceProto.TimeControl.TIME_CONTROL_CLASSICAL_30_0
}
