package com.bear.implementasimpleatmcontroller.repository

import com.bear.implementasimpleatmcontroller.domain.ATMSession
import com.bear.implementasimpleatmcontroller.domain.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ATMSessionRepository : JpaRepository<ATMSession, Long> {
    fun findBySessionId(sessionId: String): ATMSession?
    fun findBySessionIdAndSessionStatusNot(sessionId: String, status: SessionStatus): ATMSession?
}