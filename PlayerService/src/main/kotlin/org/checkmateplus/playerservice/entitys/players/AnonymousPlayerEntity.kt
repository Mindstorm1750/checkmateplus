package org.checkmateplus.playerservice.entitys.players

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.playerModels.PlayerPreferences
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import jakarta.persistence.Transient
@Entity
@DiscriminatorValue("ANONYMOUS")
class AnonymousPlayerEntity(
    playerId: String,
    displayName: String,
    joinedQueueAt: Long,
    skillRating: Int? = null,
    ratingDeviation: Double? = null,
    volatility: Double? = null,
    @Column(name = "player_preferences", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val playerPreferencesJson: String? = null
) : PlayerEntity(playerId, displayName, joinedQueueAt, skillRating, ratingDeviation, volatility) {
    @get:Transient
    val playerPreferences: PlayerPreferences?
        get() = playerPreferencesJson?.let { Json.decodeFromString(it) }
}