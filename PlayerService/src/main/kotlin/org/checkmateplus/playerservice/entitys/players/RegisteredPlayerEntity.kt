package org.checkmateplus.playerservice.entitys.players

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.Transient
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.playerModels.PlayerPreferences
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@DiscriminatorValue("REGISTERED")
class RegisteredPlayerEntity(
    playerId: String,
    displayName: String,
    joinedQueueAtEpochMs: Long,
    skillRating: Int?,
    @Column(name = "player_preferences")
    @JdbcTypeCode(SqlTypes.JSON)
    val playerPreferencesJson: String?

) : PlayerEntity(playerId, displayName, joinedQueueAtEpochMs, skillRating) {

    @get:Transient
    val playerPreferences: PlayerPreferences?
        get() = playerPreferencesJson?.let { Json.decodeFromString(it) }
}