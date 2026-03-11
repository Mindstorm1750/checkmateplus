package org.checkmateplus.playerservice.services

import kotlinx.serialization.json.Json
import org.checkmateplus.playerservice.entitys.AnonymousPlayerEntity
import org.checkmateplus.playerservice.entitys.BotPlayerEntity
import org.checkmateplus.playerservice.entitys.PlayerEntity
import org.checkmateplus.playerservice.models.toDomain
import org.checkmateplus.playerservice.models.toEntity
import org.checkmateplus.playerservice.proto.PlayerServiceProto
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.playerservice.repository.PlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// Service
@Service
class MatchRecordService(
    private val matchRecordRepository: MatchRecordRepository,
    private val playerRepository: PlayerRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun save(record: PlayerServiceProto.MatchRecord) {
        val whitePlayer = getOrCreatePlayer(record.whitePlayer)
        val blackPlayer = getOrCreatePlayer(record.blackPlayer)

        val entity = record.toEntity(whitePlayer, blackPlayer)
        matchRecordRepository.save(entity)
        logger.info("Stored match record: ${record.matchId}")
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