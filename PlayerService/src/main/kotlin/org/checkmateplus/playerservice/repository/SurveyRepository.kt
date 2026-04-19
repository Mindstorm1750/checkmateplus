package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.SurveyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SurveyRepository : JpaRepository<SurveyEntity, String>