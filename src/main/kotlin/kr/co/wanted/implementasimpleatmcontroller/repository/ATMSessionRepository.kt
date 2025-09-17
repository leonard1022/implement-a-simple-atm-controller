package kr.co.wanted.implementasimpleatmcontroller.repository

import kr.co.wanted.implementasimpleatmcontroller.domain.ATMSession
import kr.co.wanted.implementasimpleatmcontroller.domain.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ATMSessionRepository : JpaRepository<ATMSession, Long> {
    fun findBySessionId(sessionId: String): ATMSession?
    fun findBySessionIdAndSessionStatusNot(sessionId: String, status: SessionStatus): ATMSession?
}