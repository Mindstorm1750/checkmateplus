package org.checkmateplus.playerservice.entitys

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Table
import jakarta.persistence.Transient
import kotlin.time.Instant

@Entity
@Table(name = "players")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "player_type", discriminatorType = DiscriminatorType.STRING)
abstract class PlayerEntity(
    @Id val playerId: String,
    val displayName: String,
    @Column(name = "joined_queue_at")
    val joinedQueueAtEpochMs: Long,
    val skillRating: Int?
){
    @get:Transient
    val joinedQueueAt: Instant
        get() = Instant.fromEpochMilliseconds(joinedQueueAtEpochMs)
}
