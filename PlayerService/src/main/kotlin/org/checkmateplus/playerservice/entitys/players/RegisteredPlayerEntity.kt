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
    joinedQueueAt: Long,
    skillRating: Int? = 1200,
    ratingDeviation: Double? = 350.0,
    volatility: Double? = 0.06,
    @Column(name = "player_preferences", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var playerPreferencesJson: String? = null
) : PlayerEntity(playerId, displayName, joinedQueueAt, skillRating, ratingDeviation, volatility) {
    @get:Transient
    val playerPreferences: PlayerPreferences?
        get() = playerPreferencesJson?.let { Json.decodeFromString(it) }
}