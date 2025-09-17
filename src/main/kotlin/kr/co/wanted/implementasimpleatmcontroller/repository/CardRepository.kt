package kr.co.wanted.implementasimpleatmcontroller.repository

import kr.co.wanted.implementasimpleatmcontroller.domain.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CardRepository : JpaRepository<Card, Long> {
    fun findByCardNumber(cardNumber: String): Card?
}