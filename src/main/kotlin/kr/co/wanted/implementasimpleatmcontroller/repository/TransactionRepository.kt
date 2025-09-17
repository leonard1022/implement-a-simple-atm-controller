package kr.co.wanted.implementasimpleatmcontroller.repository

import kr.co.wanted.implementasimpleatmcontroller.domain.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TransactionRepository : JpaRepository<Transaction, Long> {
}
