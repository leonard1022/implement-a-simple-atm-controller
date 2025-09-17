package kr.co.wanted.implementasimpleatmcontroller.repository

import kr.co.wanted.implementasimpleatmcontroller.domain.Account
import kr.co.wanted.implementasimpleatmcontroller.domain.Card
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByAccountNumber(accountNumber: String): Account?
    fun findByCard(card: Card): List<Account>
}