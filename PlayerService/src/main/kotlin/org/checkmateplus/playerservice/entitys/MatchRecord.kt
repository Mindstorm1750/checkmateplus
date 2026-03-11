package org.checkmateplus.playerservice.entitys


import jakarta.persistence.*
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.playerModels.MatchStatus
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.chessLibrary.playerModels.history.MatchOutcome
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "match_records")
class MatchRecordEntity(

    @Id
    @Column(name = "match_id", nullable = false, updatable = false)
    var matchId: String,

    @Column(name = "board_state")
    @JdbcTypeCode(SqlTypes.JSON)
    var boardJson: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "time_control", nullable = false)
    var timeControl: TimeControl,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    var matchType: MatchType,

    @Column(name = "bot_configuration")
    @JdbcTypeCode(SqlTypes.JSON)
    var botConfigurationJson: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_outcome", nullable = false)
    var matchOutcome: MatchOutcome,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "white_player_id", nullable = false)
    var whitePlayer: PlayerEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "black_player_id", nullable = false)
    var blackPlayer: PlayerEntity,

    @Column(name = "remaining_white_time_ms", nullable = false)
    var remainingWhitePlayerTime: Int,

    @Column(name = "remaining_black_time_ms", nullable = false)
    var remainingBlackPlayerTime: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false)
    var matchStatus: MatchStatus,

    @Column(name = "match_history")
    @JdbcTypeCode(SqlTypes.JSON)
    var historyJson: String = "[]"
) {
    @get:Transient
    val board: Board get() = Json.decodeFromString(boardJson)

    @get:Transient
    val botConfiguration: BotConfiguration? get() = botConfigurationJson?.let { Json.decodeFromString(it) }

    @get:Transient
    val players: List<PlayerEntity?> get() = listOf(whitePlayer, blackPlayer)
}