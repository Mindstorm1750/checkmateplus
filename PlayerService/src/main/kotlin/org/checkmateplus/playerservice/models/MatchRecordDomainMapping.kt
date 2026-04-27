package org.checkmateplus.playerservice.models

import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.checkmateplus.chessLibrary.botEngine.BotModelType
import org.checkmateplus.chessLibrary.botEngine.BotMoveTimeModelType
import org.checkmateplus.chessLibrary.chessEngine.*
import org.checkmateplus.chessLibrary.playerModels.*
import org.checkmateplus.chessLibrary.playerModels.history.MatchOutcome
import org.checkmateplus.chessLibrary.playerModels.history.Turn
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.checkmateplus.playerservice.proto.PlayerServiceProto
import kotlin.time.Instant

fun PlayerServiceProto.MatchRecord.toEntity(
    whitePlayer: PlayerEntity,
    blackPlayer: PlayerEntity
): MatchRecordEntity = MatchRecordEntity(
    matchId = matchId,
    boardJson = Json.encodeToString(board.toDomain()),
    timeControl = timeControl.toDomain(),
    matchType = matchType.toDomain(),
    botConfigurationJson = if (hasBotConfiguration()) Json.encodeToString(botConfiguration.toDomain()) else null,
    matchOutcome = matchOutcome.toDomain(),
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    remainingWhitePlayerTime = remainingWhitePlayerTime.toLong(),
    remainingBlackPlayerTime = remainingBlackPlayerTime.toLong(),
    matchStatus = matchStatus.toDomain(),
    historyJson = Json.encodeToString(historyList.map { it.toDomain() }),
    createdAtEpochMs = matchCreatedAtEpochMs
)

fun PlayerServiceProto.Board.toDomain(): Board = Board(
    squares = Array(64) { i ->
        squaresList.getOrNull(i)
            ?.takeIf { it != PlayerServiceProto.Piece.getDefaultInstance() }
            ?.toDomain()
    },
    sideToMove = sideToMove.toDomain(),
    castlingRights = castlingRights.toDomain(),
    enPassantSquare = if (hasEnPassantSquare()) enPassantSquare else null,
    halfMoveClock = halfMoveClock,
    fullMoveNumber = fullMoveNumber
)

fun PlayerServiceProto.Piece.toDomain(): Piece = Piece(
    type = type.toDomain(),
    color = colour.toDomain()
)

fun PlayerServiceProto.CastlingRights.toDomain(): CastlingRights = CastlingRights(
    whiteKingSide = whiteKingside,
    whiteQueenSide = whiteQueenside,
    blackKingSide = blackKingside,
    blackQueenSide = blackQueenside
)


fun PlayerServiceProto.Move.toDomain(): Move = Move(
    from = from,
    to = to,
    promotion = if (hasPromotionPiece()) promotionPiece.toDomain() else null
)


fun PlayerServiceProto.Player.toDomain(): Player {
    val ratingDeviation = if (hasRatingDeviation()) ratingDeviation else null
    val volatility = if (hasVolatility()) volatility else null

    return when {
        hasAnonymous() -> Player.Anonymous(
            playerId = playerId,
            displayName = displayName,
            skillRating = if (hasSkillRating()) skillRating else null,
            ratingDeviation = ratingDeviation,
            volatility = volatility,
            joinedQueueAt = Instant.fromEpochMilliseconds(joinedQueueAtEpochMs),
            playerPreferences = if (anonymous.hasPlayerPreferences())
                anonymous.playerPreferences.toDomain() else null
        )
        hasBot() -> Player.Bot(
            playerId = playerId,
            displayName = displayName,
            skillRating = if (hasSkillRating()) skillRating else null,
            ratingDeviation = ratingDeviation,
            volatility = volatility,
            joinedQueueAt = Instant.fromEpochMilliseconds(joinedQueueAtEpochMs),
            difficulty = bot.difficulty,
            botConfiguration = bot.botConfiguration.toDomain()
        )
        hasRegistered() -> Player.Registered(
            playerId = playerId,
            displayName = displayName,
            skillRating = if (hasSkillRating()) skillRating else null,
            ratingDeviation = ratingDeviation,
            volatility = volatility,
            joinedQueueAt = Instant.fromEpochMilliseconds(joinedQueueAtEpochMs),
            playerPreferences = if (registered.hasPlayerPreferences())
                registered.playerPreferences.toDomain() else null
        )
        else -> throw IllegalArgumentException("Unknown player type for player: $playerId")
    }
}

fun PlayerServiceProto.PlayerPreferences.toDomain(): PlayerPreferences = PlayerPreferences(
    boardColour = boardColour,
    animationType = animationType,
)



fun GameServerProto.BotConfiguration.toDomain(): BotConfiguration = BotConfiguration(
    botModelType = botModelType.toDomain(),
    botMoveTimeModelType = botMoveTimeModelType.toDomain()
)

fun GameServerProto.BotModelType.toDomain(): BotModelType = when (this) {
    GameServerProto.BotModelType.RANDOM  -> BotModelType.RANDOM
    GameServerProto.BotModelType.MINMAX -> BotModelType.MINMAX
    else -> throw IllegalArgumentException("Unknown BotModelType: $this")
}

fun GameServerProto.BotMoveTimeModelType.toDomain(): BotMoveTimeModelType = when (this) {
    GameServerProto.BotMoveTimeModelType.NODELAY  -> BotMoveTimeModelType.NODELAY
    GameServerProto.BotMoveTimeModelType.MEDIAN   -> BotMoveTimeModelType.MEDIAN
    GameServerProto.BotMoveTimeModelType.REALISTIC -> BotMoveTimeModelType.REALISTIC
    else -> throw IllegalArgumentException("Unknown BotMoveTimeModelType: $this")
}


fun PlayerServiceProto.Turn.toDomain(): Turn = Turn(
    player = player.toDomain(),
    move = move.toDomain(),
    board = board.toDomain(),
    fen = fen,
    timeTaken = timeTaken
)



fun PlayerServiceProto.Colour.toDomain(): Colour = when (this) {
    PlayerServiceProto.Colour.COLOUR_WHITE -> Colour.WHITE
    PlayerServiceProto.Colour.COLOUR_BLACK -> Colour.BLACK
    else -> throw IllegalArgumentException("Unknown Colour: $this")
}

fun PlayerServiceProto.PieceType.toDomain(): PieceType = when (this) {
    PlayerServiceProto.PieceType.PIECE_TYPE_PAWN   -> PieceType.PAWN
    PlayerServiceProto.PieceType.PIECE_TYPE_KNIGHT -> PieceType.KNIGHT
    PlayerServiceProto.PieceType.PIECE_TYPE_BISHOP -> PieceType.BISHOP
    PlayerServiceProto.PieceType.PIECE_TYPE_ROOK   -> PieceType.ROOK
    PlayerServiceProto.PieceType.PIECE_TYPE_QUEEN  -> PieceType.QUEEN
    PlayerServiceProto.PieceType.PIECE_TYPE_KING   -> PieceType.KING
    else -> throw IllegalArgumentException("Unknown PieceType: $this")
}

fun PlayerServiceProto.MatchOutcome.toDomain(): MatchOutcome = when (this) {
    PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_WHITE_WINS -> MatchOutcome.WHITE_WINS
    PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_BLACK_WINS -> MatchOutcome.BLACK_WINS
    PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_DRAW       -> MatchOutcome.DRAW
    PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_ABANDONED  -> MatchOutcome.ABANDONED
    else -> throw IllegalArgumentException("Unknown MatchOutcome: $this")
}

fun GameServerProto.MatchStatus.toDomain(): MatchStatus = when (this) {
    GameServerProto.MatchStatus.MATCH_STATUS_PENDING   -> MatchStatus.PENDING
    GameServerProto.MatchStatus.MATCH_STATUS_ASSIGNED  -> MatchStatus.ASSIGNED
    GameServerProto.MatchStatus.MATCH_STATUS_ACTIVE    -> MatchStatus.ACTIVE
    GameServerProto.MatchStatus.MATCH_STATUS_COMPLETED -> MatchStatus.COMPLETED
    GameServerProto.MatchStatus.MATCH_STATUS_CANCELLED -> MatchStatus.CANCELLED
    GameServerProto.MatchStatus.MATCH_STATUS_ABANDONED -> MatchStatus.ABANDONED
    else -> throw IllegalArgumentException("Unknown MatchStatus: $this")
}

fun PlayerServiceProto.MatchType.toDomain(): MatchType = when (this) {
    PlayerServiceProto.MatchType.MATCH_TYPE_CASUAL_PVP      -> MatchType.CASUAL_PVP
    PlayerServiceProto.MatchType.MATCH_TYPE_PVE             -> MatchType.PVE
    PlayerServiceProto.MatchType.MATCH_TYPE_COMPETITIVE_PVP -> MatchType.COMPETITIVE_PVP
    else -> throw IllegalArgumentException("Unknown MatchType: $this")
}

fun PlayerServiceProto.TimeControl.toDomain(): TimeControl = when (this) {
    PlayerServiceProto.TimeControl.TIME_CONTROL_BULLET_1_0    -> TimeControl.BULLET_1_0
    PlayerServiceProto.TimeControl.TIME_CONTROL_BULLET_2_1    -> TimeControl.BULLET_2_1
    PlayerServiceProto.TimeControl.TIME_CONTROL_BLITZ_3_0     -> TimeControl.BLITZ_3_0
    PlayerServiceProto.TimeControl.TIME_CONTROL_BLITZ_5_0     -> TimeControl.BLITZ_5_0
    PlayerServiceProto.TimeControl.TIME_CONTROL_RAPID_10_0    -> TimeControl.RAPID_10_0
    PlayerServiceProto.TimeControl.TIME_CONTROL_RAPID_15_10   -> TimeControl.RAPID_15_10
    PlayerServiceProto.TimeControl.TIME_CONTROL_CLASSICAL_30_0 -> TimeControl.CLASSICAL_30_0
    else -> throw IllegalArgumentException("Unknown TimeControl: $this")
}