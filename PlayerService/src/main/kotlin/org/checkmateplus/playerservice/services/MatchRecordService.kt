package org.checkmateplus.playerservice.services

import kotlinx.serialization.json.Json
import org.checkmateplus.playerservice.entitys.players.AnonymousPlayerEntity
import org.checkmateplus.playerservice.entitys.players.BotPlayerEntity
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.checkmateplus.playerservice.models.toDomain
import org.checkmateplus.playerservice.models.toEntity
import org.checkmateplus.playerservice.proto.PlayerServiceProto
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.playerservice.repository.PlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MatchRecordService(
    private val matchRecordRepository: MatchRecordRepository,
    private val playerRepository: PlayerRepository,
    private val glicko2Service: Glicko2Service
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(record: PlayerServiceProto.MatchRecord) {
        val whitePlayer = getOrCreatePlayer(record.whitePlayer)
        val blackPlayer = getOrCreatePlayer(record.blackPlayer)

        val entity = record.toEntity(whitePlayer, blackPlayer)
        matchRecordRepository.save(entity)
        logger.info("Stored match record: ${record.matchId}")

        val isCompetitive = record.matchType == PlayerServiceProto.MatchType.MATCH_TYPE_COMPETITIVE_PVP
        val isDefinitive = record.matchOutcome != PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_ABANDONED
        if (isCompetitive && isDefinitive) {
            updateRatings(whitePlayer, blackPlayer, record.matchOutcome)
        }
    }

    private fun updateRatings(
        white: PlayerEntity,
        black: PlayerEntity,
        outcome: PlayerServiceProto.MatchOutcome
    ) {
        val whiteReg = white as? RegisteredPlayerEntity ?: return
        val blackReg = black as? RegisteredPlayerEntity ?: return

        val wRating = whiteReg.skillRating ?: return
        val wRD = whiteReg.ratingDeviation ?: return
        val wVol = whiteReg.volatility ?: return
        val bRating = blackReg.skillRating ?: return
        val bRD = blackReg.ratingDeviation ?: return
        val bVol = blackReg.volatility ?: return

        val glickoOutcome = when (outcome) {
            PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_WHITE_WINS -> Glicko2Service.Outcome.WHITE_WINS
            PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_BLACK_WINS -> Glicko2Service.Outcome.BLACK_WINS
            PlayerServiceProto.MatchOutcome.MATCH_OUTCOME_DRAW -> Glicko2Service.Outcome.DRAW
            else -> return
        }

        val (newWhite, newBlack) = glicko2Service.updateRatings(
            wRating, wRD, wVol,
            bRating, bRD, bVol,
            glickoOutcome
        )

        whiteReg.skillRating = newWhite.rating
        whiteReg.ratingDeviation = newWhite.ratingDeviation
        whiteReg.volatility = newWhite.volatility

        blackReg.skillRating = newBlack.rating
        blackReg.ratingDeviation = newBlack.ratingDeviation
        blackReg.volatility = newBlack.volatility

        playerRepository.save(whiteReg)
        playerRepository.save(blackReg)

        logger.info(
            "Ratings updated — white: $wRating → ${newWhite.rating} (RD ${wRD.toInt()} → ${newWhite.ratingDeviation.toInt()}), " +
            "black: $bRating → ${newBlack.rating} (RD ${bRD.toInt()} → ${newBlack.ratingDeviation.toInt()})"
        )
    }

    private fun getOrCreatePlayer(proto: PlayerServiceProto.Player): PlayerEntity {
        return playerRepository.findById(proto.playerId).orElseGet {
            val entity = when {
                proto.hasAnonymous() -> AnonymousPlayerEntity(
                    playerId = proto.playerId,
                    displayName = proto.displayName,
                    joinedQueueAt = proto.joinedQueueAtEpochMs,
                    skillRating = if (proto.hasSkillRating()) proto.skillRating else null,
                    playerPreferencesJson = if (proto.anonymous.hasPlayerPreferences())
                        Json.encodeToString(proto.anonymous.playerPreferences.toDomain()) else null
                )
                proto.hasBot() -> BotPlayerEntity(
                    playerId = proto.playerId,
                    displayName = proto.displayName,
                    joinedQueueAt = proto.joinedQueueAtEpochMs,
                    skillRating = if (proto.hasSkillRating()) proto.skillRating else null,
                    difficulty = proto.bot.difficulty,
                    botConfigurationJson = Json.encodeToString(proto.bot.botConfiguration.toDomain())
                )
                else -> throw IllegalArgumentException("Unknown player type: ${proto.playerId}")
            }
            playerRepository.save(entity)
        }
    }
}