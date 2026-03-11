package org.checkmateplus.playerservice.entitys.players

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import jakarta.persistence.Transient
@Entity
@DiscriminatorValue("BOT")
class BotPlayerEntity(
    playerId: String,
    displayName: String,
    joinedQueueAt: Long,
    skillRating: Int?,
    val difficulty: Int,
    @Column(name = "bot_configuration")
    @JdbcTypeCode(SqlTypes.JSON)
    val botConfigurationJson: String
) : PlayerEntity(playerId, displayName, joinedQueueAt, skillRating) {
    @get:Transient
    val botConfiguration: BotConfiguration
        get() = Json.decodeFromString(botConfigurationJson)
}