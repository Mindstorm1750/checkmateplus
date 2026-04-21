package org.checkmateplus.playerservice.entitys

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "surveys")
class SurveyEntity(

    @Id
    @Column(name = "survey_id", nullable = false, updatable = false)
    var surveyId: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    var matchRecord: MatchRecordEntity,

    @Column(name = "survey_score", nullable = false)
    var surveyScore: Int,

    @Column(name = "optional_comments")
    var optionalComments: String? = null
)